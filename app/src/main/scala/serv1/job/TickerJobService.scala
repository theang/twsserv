package serv1.job

import akka.actor.typed.ActorRef
import serv1.client.converters.BarSizeConverter
import serv1.client.model.TickerTickLastExchange
import serv1.client.{DataClient, TWSClientErrors}
import serv1.db.TickerDataActor
import serv1.db.TickerDataActor.{Retry, Write, WriteTickBidAsk, WriteTickLast}
import serv1.db.repo.intf.{ExchangeRepoIntf, JobRepoIntf}
import serv1.db.schema.TickerTickBidAsk
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import serv1.util.LocalDateTimeUtil
import slick.util.Logging

import java.time.LocalDateTime
import java.util.UUID


class TickerJobService(client: DataClient,
                       jobRepo: JobRepoIntf,
                       tickerDataActor: ActorRef[TickerDataActor.Message],
                       exchangeRepo: ExchangeRepoIntf) extends Logging {

  var ERROR_CODES_TO_EXCLUDE_TICKERS_FROM_FURTHER_PROCESSING: Set[Int] = Set(TWSClientErrors.CONTRACT_DESCRIPTION_IS_AMBIGUOUS)

  var tickerJobActor: Option[ActorRef[TickerJobActor.JobActorMessage]] = None

  val DEFAULT_EXCHANGE: String = "DEFAULT"

  def updateJob(jobId: UUID, ticker: TickerLoadType, error: Option[String], ignore: Boolean): Boolean = {
    val updateResult = jobRepo.updateJob(jobId, ticker, error, ignore)
    if (updateResult && (ignore || error.isEmpty)) {
      tickerJobActor.foreach { tickerJobActorRef =>
        tickerJobActorRef ! TickerJobActor.Finished(jobId)
      }
    }
    updateResult
  }

  def updateJobWithRetry(jobId: UUID, ticker: TickerLoadType, error: Option[String], ignore: Boolean): Unit = {
    tickerDataActor ! Retry(0, s"Updating job $jobId for $ticker error $error ignore $ignore", () => updateJob(jobId, ticker, error, ignore), null)
  }

  def saveHistoricalData(jobId: UUID, ticker: TickerLoadType, data: Seq[HistoricalData], overwrite: Boolean, last: Boolean): Unit = {
    logger.info(s"writing ${data.size} $ticker $last")
    tickerDataActor ! Write(1, ticker = ticker, historicalData = data, overwrite = overwrite, replyTo = null)
    if (last) {
      updateJobWithRetry(jobId, ticker, Option.empty, ignore = false)
    }
  }

  def errorCallback(jobId: UUID, ticker: TickerLoadType, code: Int, msg: String, advancedOrderRejectJson: String): Unit = {
    logger.error(s"Client error: code = $code, message = $msg, advancedOrderRejectJson = $advancedOrderRejectJson")
    val excludeTickerFromFurtherProcessing = ERROR_CODES_TO_EXCLUDE_TICKERS_FROM_FURTHER_PROCESSING.contains(code)
    updateJobWithRetry(jobId, ticker, Option(s"$code $msg $advancedOrderRejectJson"), excludeTickerFromFurtherProcessing)
  }

  def loadTicker(jobId: UUID, ticker: TickerLoadType, from: LocalDateTime, to: LocalDateTime): Unit = {
    val overwrite = jobRepo.getTickerJobStates(jobId).head.overwrite
    client.loadHistoricalData(LocalDateTimeUtil.toEpoch(from),
      LocalDateTimeUtil.toEpoch(to),
      ticker.tickerType,
      BarSizeConverter.getBarSizeSeconds(ticker.barSize),
      (data: Seq[HistoricalData], last: Boolean) => saveHistoricalData(jobId, ticker, data, overwrite, last),
      (code: Int, msg: String, advancedOrderRejectJson: String) => errorCallback(jobId, ticker, code, msg, advancedOrderRejectJson))
  }

  def loadTickers(jobId: UUID, tickers: List[TickerLoadType],
                  from: LocalDateTime,
                  to: LocalDateTime): Unit = tickers.foreach {
    loadTicker(jobId, _, from, to)
  }

  def startTickLoading(jobId: UUID, ticker: TickerLoadType): (Int, Int) = {
    client.startLoadingTickData(ticker.tickerType,
      (rawTick: Seq[TickerTickLastExchange], _) => {
        val ticks = rawTick.map { tick =>
          tick.tickerTick.copy(exch = exchangeRepo.getExchangeId(
            if (tick.exch == null) DEFAULT_EXCHANGE else tick.exch
          ), spec = Option(tick.tickerTick.spec).getOrElse(""))
        }
        tickerDataActor ! WriteTickLast(1, ticker = ticker, historicalData = ticks, replyTo = null)
      }, (ticks: Seq[TickerTickBidAsk], _) => {
        tickerDataActor ! WriteTickBidAsk(1, ticker = ticker, historicalData = ticks, replyTo = null)
      }, (code: Int, msg: String, advancedOrderRejectJson: String) => errorCallback(jobId, ticker, code, msg, advancedOrderRejectJson))
  }

  def stopTickLoading(jobId: UUID, reqNumberLast: Int, reqNumberBidAsk: Int): Unit = {
    client.cancelLoadingTickData(reqNumberLast, reqNumberBidAsk)
    tickerDataActor ! Retry(0, s"Set FINISHED state for job $jobId", () => {
      jobRepo.cancelTickLoadingJob(jobId)
      true
    }, null)
  }
}

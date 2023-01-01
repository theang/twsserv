package serv1.job

import akka.actor.typed.ActorRef
import serv1.client.DataClient
import serv1.client.converters.BarSizeConverter
import serv1.db.TickerDataActor
import serv1.db.TickerDataActor.Write
import serv1.db.repo.intf.JobRepoIntf
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import serv1.util.LocalDateTimeUtil
import slick.util.Logging

import java.time.LocalDateTime
import java.util.UUID


class TickerJobService(client: DataClient,
                       jobRepo: JobRepoIntf,
                       tickerDataActor: ActorRef[TickerDataActor.Write]) extends Logging {

  def saveHistoricalData(jobId: UUID, ticker: TickerLoadType, data: Seq[HistoricalData], last: Boolean): Unit = {
    logger.debug(s"writing ${data.size}")
    tickerDataActor ! Write(1, ticker = ticker, historicalData = data, replyTo = null)
    if (last) {
      jobRepo.updateJob(jobId, ticker)
    }
  }

  def errorCallback(jobId: UUID, ticker: TickerLoadType, code: Int, msg: String): Unit = {
    logger.error(s"Client error: code = $code, message = $msg")
    jobRepo.updateJob(jobId, ticker, Option(s"$code $msg"))
  }

  def loadTicker(jobId: UUID, ticker: TickerLoadType, from: LocalDateTime, to: LocalDateTime): Unit = {
    client.loadHistoricalData(LocalDateTimeUtil.toEpoch(from),
      LocalDateTimeUtil.toEpoch(to),
      ticker.tickerType.name,
      ticker.tickerType.exchange,
      ticker.tickerType.typ,
      BarSizeConverter.getBarSizeSeconds(ticker.barSize),
      ticker.tickerType.prec,
      (data: Seq[HistoricalData], last: Boolean) => saveHistoricalData(jobId, ticker, data, last),
      (code: Int, msg: String) => errorCallback(jobId, ticker, code, msg))
  }

  def loadTickers(jobId: UUID, tickers: List[TickerLoadType],
                  from: LocalDateTime,
                  to: LocalDateTime): Unit = tickers.foreach {
    loadTicker(jobId, _, from, to)
  }
}

package serv1.rest.services.loaddata

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import serv1.db.repo.impl.JobRepo
import serv1.db.repo.intf.TickerDataRepoIntf
import serv1.job.TickerJobActor
import serv1.model.job.TickerJobState
import serv1.model.ticker.TickerLoadType
import serv1.rest.JsonFormats
import serv1.util.LocalDateTimeUtil
import slick.util.Logging

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration._

class LoadService(tickerDataRepoIntf: TickerDataRepoIntf,
                  tickerJobActorRef: ActorRef[TickerJobActor.JobActorMessage])(implicit system: ActorSystem[_]) extends JsonFormats with Logging {
  implicit val timeout: Timeout = 1000.seconds

  def load(ticker: Seq[TickerLoadType], from: LocalDateTime, to: LocalDateTime, overwrite: Boolean): UUID = {
    val jobId = JobRepo.createTickerJob(ticker, from, to, overwrite)
    tickerJobActorRef.ask(replyTo => TickerJobActor.Run(jobId, replyTo))
    jobId
  }

  /** *
   * Reloads tickers' data
   * Calling this will erase previously loaded bars and rewrite them
   *
   * @param ticker tickers to load
   * @return created job Ids
   */
  def load(ticker: Seq[TickerLoadType]): Seq[UUID] = {
    val tickerFromTo = ticker.groupBy({ ticker =>
      val from = tickerDataRepoIntf.earliestDate(ticker)
      val to = tickerDataRepoIntf.latestDate(ticker)
      (from, to)
    })
    val result = tickerFromTo.flatMap {
      case ((Some(from), Some(to)), tickers) =>
        logger.info(s"$tickers : running $tickers from $from")
        Option.apply(load(tickers,
          LocalDateTimeUtil.fromEpoch(from),
          LocalDateTimeUtil.fromEpoch(to), overwrite = true))
      case _ => Option.empty
    }
    result.toSeq
  }

  def checkJobStates(id: UUID): List[TickerJobState] = {
    JobRepo.getTickerJobStates(id).toList
  }

  def startTickLoad(tickers: Seq[TickerLoadType]): Seq[UUID] = {
    if (tickers.length != 1) {
      logger.error(s"Only first ticker will be used $tickers")
    }
    val jobId = JobRepo.createTickLoadingJob(tickers)
    tickerJobActorRef.ask(replyTo => TickerJobActor.Run(jobId, replyTo))
    Seq(jobId)
  }

  def stopTickLoad(tickers: Seq[TickerLoadType]): Boolean = {
    if (tickers.length != 1) {
      logger.error(s"Only first ticker will be used $tickers")
    }
    JobRepo.findTickLoadingJobByLoadType(tickers.head).map { id =>
      JobRepo.cancelTickLoadingJob(id)
      tickerJobActorRef ! TickerJobActor.Finished(id)
    }.nonEmpty
  }
}

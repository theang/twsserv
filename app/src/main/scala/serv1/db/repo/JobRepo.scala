package serv1.db.repo

import serv1.Configuration
import serv1.db.DB
import serv1.db.schema.{Job, JobTable}
import serv1.job.{JobState, TickerJobState}
import serv1.model.job.JobStatuses
import serv1.model.ticker.TickerLoadType
import serv1.rest.JsonFormats

import java.util.UUID
import slick.jdbc.PostgresProfile.api._
import spray.json._

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object JobRepo extends JsonFormats {
  def createTickerJob(tickersToLoad: List[TickerLoadType], from: LocalDateTime, to: LocalDateTime): UUID = {
    val id = UUID.randomUUID()
    DB.db.run(DBIO.seq(JobTable.query += Job(id,
      TickerJobState(JobStatuses.IN_PROGRESS,
        tickers = tickersToLoad, List.empty, from, to).toJson.compactPrint)))
    id
  }

  def getTickerJobs(jobId : UUID): Seq[TickerJobState] = {
    val query = JobTable.query.filterIf(jobId != null)(_.jobId === jobId)
    val jobs: Seq[Job] = Await.result(DB.db.run[Seq[Job]](query.result), Configuration.callDuration)
    jobs.map { job =>
      val st: JobState = job.state.parseJson.convertTo[JobState]
      st match {
        case t: TickerJobState =>
          t
        case default =>
          null
      }
    }.filter {
      _ != null
    }.toList
  }

  def updateTickerJobState(t: TickerJobState, ticker: TickerLoadType): TickerJobState = {
    val newTicker = t.tickers diff List(ticker)
    val newLoadedTicker = t.loadedTickers ++ List(ticker)
    t.copy(status = if (newTicker.isEmpty) JobStatuses.FINISHED else JobStatuses.IN_PROGRESS,
      tickers = newTicker, loadedTickers = newLoadedTicker,
      from = t.from, to = t.to)
  }

  def updateJob(jobId: UUID, ticker: TickerLoadType): Unit = {
    val query = JobTable.query.filter(_.jobId === jobId)
    val action = query.result.flatMap {
      case t: TickerJobState =>
        query.map(_.state).update(updateTickerJobState(t, ticker).toJson.compactPrint)
      case st@default =>
        DBIO.failed(new Exception("Can update only Ticker Job"))
    }.transactionally
    DB.db.run(action)
  }
}

package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.repo.intf.JobRepoIntf
import serv1.db.schema.{Job, JobTable}
import serv1.job.{JobState, TickerJobState}
import serv1.model.job.JobStatuses
import serv1.model.ticker.{TickerError, TickerLoadType}
import serv1.rest.JsonFormats
import slick.jdbc.PostgresProfile.api._
import spray.json._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object JobRepo extends JsonFormats with JobRepoIntf {
  def createTickerJob(tickersToLoad: Seq[TickerLoadType], from: LocalDateTime, to: LocalDateTime): UUID = {
    val id = UUID.randomUUID()
    Await.result(DB.db.run(DBIO.seq(JobTable.query += Job(id,
      TickerJobState(JobStatuses.IN_PROGRESS,
        tickers = tickersToLoad.toList, List.empty, List.empty, from, to).asInstanceOf[JobState].toJson.prettyPrint))), Duration.Inf)
    id
  }

  def getTickerJobs(jobId: UUID): Seq[TickerJobState] = {
    val query = JobTable.query.filterIf(jobId != null)(_.jobId === jobId)
    val jobs: Seq[Job] = Await.result(DB.db.run[Seq[Job]](query.result), Duration.Inf)
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

  def updateTickerJobState(t: TickerJobState, ticker: TickerLoadType, error: Option[String]): TickerJobState = {
    val newTicker = t.tickers diff List(ticker)
    val newLoadedTicker = if (error.isEmpty) {
      t.loadedTickers ++ List(ticker)
    } else t.loadedTickers
    val newErrors = if (error.isDefined) {
      t.errors ++ List(TickerError(ticker, error.get))
    } else t.errors
    t.copy(status = if (newTicker.isEmpty) JobStatuses.FINISHED else JobStatuses.IN_PROGRESS,
      tickers = newTicker, loadedTickers = newLoadedTicker,
      errors = newErrors,
      from = t.from, to = t.to)
  }

  def updateJob(jobId: UUID, ticker: TickerLoadType): Unit =
    updateJob(jobId, ticker, Option.empty)

  def updateJob(jobId: UUID, ticker: TickerLoadType, error: Option[String]): Unit = {
    val query = JobTable.query.filter(_.jobId === jobId)
    val action = query.result.headOption.flatMap {
      case Some(job) =>
        val st: JobState = job.state.parseJson.convertTo[JobState]
        st match {
          case t: TickerJobState =>
            query.map(_.state).update(updateTickerJobState(t, ticker, error).asInstanceOf[JobState].toJson.prettyPrint)
          case default =>
            DBIO.failed(new Exception("Can update only Ticker Job"))
        }
      case None =>
        DBIO.failed(new Exception(s"No job found for id: $jobId"))
    }.transactionally
    Await.result(DB.db.run(action), Duration.Inf)
  }

  def removeJob(jobId: UUID): Unit = {
    Await.result(DB.db.run(JobTable.query.filter(_.jobId === jobId).delete), Duration.Inf)
  }
}

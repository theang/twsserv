package serv1.db.repo.impl

import serv1.Configuration
import serv1.db.DBSelectRawForUpdate.{AlreadyLocked, NothingToLock, Success}
import serv1.db.repo.intf.JobRepoIntf
import serv1.db.schema.{Job, JobTable}
import serv1.db.{DB, DBSelectRawForUpdate}
import serv1.job.{JobState, TickerJobState}
import serv1.model.job.JobStatuses
import serv1.model.ticker.{TickerError, TickerLoadType}
import serv1.rest.JsonFormats
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.util.Logging
import spray.json._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object JobRepo extends JsonFormats with JobRepoIntf with Logging {

  var waitLockDurationSec: Int = Configuration.INITIAL_JOB_REPO_WAIT_LOCK_DURATION

  def archiveJob(id: UUID, tickerJobState: TickerJobState): Int = {
    Await.result(DB.db.run(JobTable.archiveQuery += Job(id, tickerJobState.asInstanceOf[JobState].toJson.prettyPrint)), Duration.Inf)
    Await.result(DB.db.run(JobTable.query.filter(_.jobId === id).delete), Duration.Inf)
  }

  def archiveCompletedJobs(): Unit = {
    getTickerJobs(null).filter {
      case (id, tickerJobState) =>
        tickerJobState.status == JobStatuses.FINISHED
      case _ => false
    }.foreach {
      case (id, tickerJobState) =>
        archiveJob(id, tickerJobState)
    }
  }

  def createTickerJob(tickersToLoad: Seq[TickerLoadType], from: LocalDateTime, to: LocalDateTime, overwrite: Boolean): UUID = {
    val id = UUID.randomUUID()
    Await.result(DB.db.run(DBIO.seq(JobTable.query += Job(id,
      TickerJobState(JobStatuses.IN_PROGRESS,
        tickers = tickersToLoad.toList, List.empty, List.empty, List.empty, from, to, overwrite).asInstanceOf[JobState].toJson.prettyPrint))), Duration.Inf)
    id
  }

  def getTickerJobs(jobId: UUID): Seq[(UUID, TickerJobState)] = {
    val query = JobTable.query.filterIf(jobId != null)(_.jobId === jobId)
    val jobs: Seq[Job] = Await.result(DB.db.run[Seq[Job]](query.result), Duration.Inf)
    jobs.map { job =>
      val st: JobState = job.state.parseJson.convertTo[JobState]
      st match {
        case t: TickerJobState =>
          (job.jobId, t)
        case default =>
          null
      }
    }.filter {
      _ != null
    }.toList
  }

  def getTickerJobStates(jobId: UUID): Seq[TickerJobState] = {
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

  def getTickerJobsByStates(jobStatuses: Set[JobStatuses.JobStatus]): Seq[(UUID, TickerJobState)] = {
    getTickerJobs(null).filter {
      case (id, tickerJobState) =>
        jobStatuses.contains(tickerJobState.status)
    }
  }

  def updateTickerJobState(t: TickerJobState, ticker: TickerLoadType, error: Option[String], ignore: Boolean): TickerJobState = {
    // remove ticker only when no error of forced to ignore
    val newTicker = if (error.isEmpty || ignore) {
      t.tickers diff List(ticker)
    } else {
      t.tickers
    }
    val newLoadedTicker = if (error.isEmpty) {
      t.loadedTickers ++ List(ticker)
    } else t.loadedTickers
    val newErrors = if (error.isDefined) {
      t.errors ++ List(TickerError(ticker, error.get))
    } else t.errors
    val newIgnoredTickers = if (ignore) {
      t.ignoredTickers ++ List(ticker)
    } else t.ignoredTickers
    t.copy(status = if (newTicker.isEmpty) JobStatuses.FINISHED else JobStatuses.IN_PROGRESS,
      tickers = newTicker, loadedTickers = newLoadedTicker,
      ignoredTickers = newIgnoredTickers,
      errors = newErrors,
      from = t.from, to = t.to)
  }

  def updateJob(jobId: UUID, ticker: TickerLoadType): Boolean =
    updateJob(jobId, ticker, Option.empty, ignore = false)


  def updateJob(jobId: UUID, ticker: TickerLoadType, error: Option[String], ignore: Boolean): Boolean = {
    logger.info(s"Updating job $jobId for ticker, $ticker error = $error, ignore = $ignore")

    val query = JobTable.query.filter(_.jobId === jobId)

    implicit val db: PostgresProfile.backend.Database = DB.db
    Await.result(new DBSelectRawForUpdate(query).apply({ job =>
      val st: JobState = job.state.parseJson.convertTo[JobState]
      st match {
        case t: TickerJobState =>
          query.map(_.state).update(updateTickerJobState(t, ticker, error, ignore).asInstanceOf[JobState].toJson.prettyPrint)
        case default =>
          DBIO.failed(new Exception("Can update only Ticker Job"))
      }
    }, waitLockDurationSec), Duration.Inf) match {
      case DBSelectRawForUpdate.Failure(reason) =>
        reason match {
          case AlreadyLocked =>
            false
          case NothingToLock =>
            throw new Exception(s"No job found for id: $jobId")
        }
      case Success(_) =>
        true
    }
  }

  def removeJob(jobId: UUID): Unit = {
    Await.result(DB.db.run(JobTable.query.filterIf(jobId != null)(_.jobId === jobId).delete), Duration.Inf)
  }
}

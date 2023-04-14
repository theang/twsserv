package serv1.db.repo.impl

import serv1.db.DBSelectRawForUpdate.{AlreadyLocked, NothingToLock, Success}
import serv1.db.repo.intf.JobRepoIntf
import serv1.db.schema.{Job, JobTable}
import serv1.db.{Configuration, DB, DBJsonFormats, DBSelectRawForUpdate}
import serv1.model.job.{JobState, JobStatuses, TickLoadingJobState, TickerJobState}
import serv1.model.ticker.{TickerError, TickerLoadType}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.util.Logging
import spray.json._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object JobRepo extends DBJsonFormats with JobRepoIntf with Logging {

  var waitLockDurationSec: Int = Configuration.INITIAL_JOB_REPO_WAIT_LOCK_DURATION

  def archiveJob[T <: JobState](id: UUID, tickerJobState: T): Int = {
    Await.result(DB.db.run(JobTable.archiveQuery += Job(id, tickerJobState.asInstanceOf[JobState].toJson.prettyPrint)), Duration.Inf)
    Await.result(DB.db.run(JobTable.query.filter(_.jobId === id).delete), Duration.Inf)
  }

  def archiveCompletedJobs(): Unit = {
    getTickerJobs[JobState](null).filter {
      case (id, tickerJobState: JobState) =>
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

  def updateJob[T <: JobState](jobId: UUID, updateState: T => String, updateMessage: String, errorMessage: String): Boolean = {
    logger.info(s"Updating job $jobId $updateMessage")

    val query = JobTable.query.filter(_.jobId === jobId)

    implicit val db: PostgresProfile.backend.Database = DB.db
    Await.result(new DBSelectRawForUpdate(query).apply({ job =>
      val st: JobState = job.state.parseJson.convertTo[JobState]
      st match {
        case t: T =>
          query.map(_.state).update(updateState(t))
        case default =>
          DBIO.failed(new Exception(errorMessage))
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

  def createTickLoadingJob(tickersToLoad: Seq[TickerLoadType]): UUID = {
    val id = UUID.randomUUID()
    Await.result(
      DB.db.run(
        DBIO.seq(JobTable.query +=
          Job(id,
            TickLoadingJobState(JobStatuses.IN_PROGRESS,
              tickersToLoad.toList,
              List.empty).asInstanceOf[JobState].toJson.prettyPrint))),
      Duration.Inf)
    id
  }

  def cancelTickLoadingJob(tickLoadingJobId: UUID): Boolean = {
    updateJob(tickLoadingJobId, { t: TickLoadingJobState =>
      t.copy(status = JobStatuses.FINISHED).asInstanceOf[JobState].toJson.prettyPrint
    }, "", "Can update only Tick Loading Job")
  }

  def findTickLoadingJobByLoadType(tickerLoadType: TickerLoadType): Seq[UUID] = {
    getTickerJobs[TickLoadingJobState](null).filter {
      case (_, tickerJobState: TickLoadingJobState) =>
        tickerJobState.status == JobStatuses.IN_PROGRESS && tickerJobState.tickers.contains(tickerLoadType)
      case _ => false
    }.map {
      case (id, _) =>
        id
    }
  }


  def getTickerJobs[T <: JobState](jobId: UUID): Seq[(UUID, T)] = {
    val query = JobTable.query.filterIf(jobId != null)(_.jobId === jobId)
    val jobs: Seq[Job] = Await.result(DB.db.run[Seq[Job]](query.result), Duration.Inf)
    jobs.map { job =>
      val st: JobState = job.state.parseJson.convertTo[JobState]
      st match {
        case t: T =>
          (job.jobId, t)
        case default =>
          null
      }
    }.filter {
      _ != null
    }.toList
  }

  def getJobStates(jobId: UUID): Seq[(UUID, JobState)] = {
    val query = JobTable.query.filterIf(jobId != null)(_.jobId === jobId)
    val jobs: Seq[Job] = Await.result(DB.db.run[Seq[Job]](query.result), Duration.Inf)
    jobs.map { job =>
      val st: JobState = job.state.parseJson.convertTo[JobState]
      (job.jobId, st)
    }
  }

  def getTickerJobStates(jobId: UUID): Seq[TickerJobState] = {
    getTickerJobs[TickerJobState](jobId).map {
      case (_, tickerJobState: TickerJobState) =>
        tickerJobState
    }
  }

  def getTickerJobsByStates[T <: JobState](jobStatuses: Set[JobStatuses.JobStatus]): Seq[(UUID, T)] = {
    getTickerJobs[T](null).filter {
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
    updateJob(jobId, { t: TickerJobState =>
      updateTickerJobState(t, ticker, error, ignore).asInstanceOf[JobState].toJson.prettyPrint
    }, "$ticker error = $error, ignore = $ignore", "Can update only Ticker Job")
  }

  def removeJob(jobId: UUID): Unit = {
    Await.result(DB.db.run(JobTable.query.filterIf(jobId != null)(_.jobId === jobId).delete), Duration.Inf)
  }
}

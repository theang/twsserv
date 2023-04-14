package serv1.db.repo.intf

import serv1.model.job.{JobState, JobStatuses, TickerJobState}
import serv1.model.ticker.TickerLoadType

import java.time.LocalDateTime
import java.util.UUID

trait JobRepoIntf {

  def archiveCompletedJobs(): Unit

  def createTickerJob(tickersToLoad: Seq[TickerLoadType], from: LocalDateTime, to: LocalDateTime, overwrite: Boolean): UUID

  def getTickerJobsByStates[T <: JobState](jobStatuses: Set[JobStatuses.JobStatus]): Seq[(UUID, T)]

  def getTickerJobStates(jobId: UUID): Seq[TickerJobState]

  def getTickerJobs[T <: JobState](jobId: UUID): Seq[(UUID, T)]

  def getJobStates(jobId: UUID): Seq[(UUID, JobState)]

  def updateTickerJobState(t: TickerJobState, ticker: TickerLoadType, error: Option[String], ignore: Boolean): TickerJobState

  def updateJob(jobId: UUID, ticker: TickerLoadType, error: Option[String], ingore: Boolean): Boolean

  def updateJob(jobId: UUID, ticker: TickerLoadType): Boolean

  def removeJob(jobId: UUID): Unit

  def cancelTickLoadingJob(tickLoadingJobId: UUID): Boolean

  def findTickLoadingJobByLoadType(tickerLoadType: TickerLoadType): Seq[UUID]
}

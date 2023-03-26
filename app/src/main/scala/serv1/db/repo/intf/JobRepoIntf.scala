package serv1.db.repo.intf

import serv1.job.TickerJobState
import serv1.model.job.JobStatuses
import serv1.model.ticker.TickerLoadType

import java.time.LocalDateTime
import java.util.UUID

trait JobRepoIntf {

  def archiveCompletedJobs(): Unit

  def createTickerJob(tickersToLoad: Seq[TickerLoadType], from: LocalDateTime, to: LocalDateTime, overwrite: Boolean): UUID

  def getTickerJobsByStates(jobStatuses: Set[JobStatuses.JobStatus]): Seq[(UUID, TickerJobState)]

  def getTickerJobStates(jobId: UUID): Seq[TickerJobState]

  def updateTickerJobState(t: TickerJobState, ticker: TickerLoadType, error: Option[String], ignore: Boolean): TickerJobState

  def updateJob(jobId: UUID, ticker: TickerLoadType, error: Option[String], ingore: Boolean): Boolean

  def updateJob(jobId: UUID, ticker: TickerLoadType): Boolean

  def removeJob(jobId: UUID): Unit
}

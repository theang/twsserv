package serv1.db.repo.intf

import serv1.job.TickerJobState
import serv1.model.ticker.TickerLoadType

import java.time.LocalDateTime
import java.util.UUID

trait JobRepoIntf {
  def createTickerJob(tickersToLoad: List[TickerLoadType], from: LocalDateTime, to: LocalDateTime): UUID

  def getTickerJobs(jobId: UUID): Seq[TickerJobState]

  def updateTickerJobState(t: TickerJobState, ticker: TickerLoadType): TickerJobState

  def updateJob(jobId: UUID, ticker: TickerLoadType): Unit

  def removeJob(jobId: UUID): Unit
}

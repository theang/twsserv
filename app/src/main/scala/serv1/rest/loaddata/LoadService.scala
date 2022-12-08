package serv1.rest.loaddata

import akka.actor.typed.ActorRef
import serv1.db.repo.JobRepo
import serv1.job.TickerJobState
import serv1.job.TickerJobActor
import serv1.model.ticker.TickerLoadType
import serv1.rest.JsonFormats

import java.time.LocalDateTime
import java.util.UUID

class LoadService(tickerJobActorRef: ActorRef[TickerJobActor.Message]) extends JsonFormats {
  def load(ticker: List[TickerLoadType], from: LocalDateTime, to: LocalDateTime): UUID = {
    val jobId = JobRepo.createTickerJob(ticker, from, to)
    tickerJobActorRef ! TickerJobActor.Run(jobId)
    jobId
  }

  def checkJobStates(id: UUID): List[TickerJobState] = {
    JobRepo.getTickerJobs(id).toList
  }
}

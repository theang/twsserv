package serv1.job

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.db.repo.intf.JobRepoIntf
import serv1.model.ticker.TickerLoadType

import java.util.UUID

object TickerJobActor {
  case class Run(jobId: UUID, replyTo: ActorRef[RunSuccessful])

  case class RunSuccessful()

  def apply(ticketJobService: TickerJobService, jobRepo: JobRepoIntf): Behavior[Run] = {
    Behaviors.receiveMessage[Run] {
      case Run(jobId, replyTo) =>
        val state = jobRepo.getTickerJobs(jobId).head
        val tickersToProcess: List[TickerLoadType] = state.tickers diff state.loadedTickers
        ticketJobService.loadTickers(jobId, tickersToProcess, state.from, state.to)
        replyTo ! RunSuccessful()
        Behaviors.same
    }
  }
}

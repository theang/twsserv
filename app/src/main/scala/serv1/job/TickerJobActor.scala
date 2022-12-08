package serv1.job

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import serv1.client.DataClient
import serv1.client.operations.ClientOperationHandlers.THREADS_NUMBER
import serv1.db.repo.JobRepo
import serv1.model.HistoricalData
import serv1.model.ticker.{TickerLoadType, TickerType}
import serv1.util.LocalDateTimeUtil
import slick.util.Logging

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object TickerJobActor {
  implicit val context: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  sealed trait Message
  private final case class StartFailed(cause: Throwable) extends Message
  case class Run(jobId:UUID) extends Message
  case object Stop extends Message

  def apply(ticketJobService: TickerJobService): Behavior[Message] = {
    Behaviors.receiveMessage[Message] {
      case StartFailed(cause) =>
        throw new RuntimeException("Server failed to start", cause)
      case Run(jobId) =>
        val state = JobRepo.getTickerJobs(jobId).head
        val tickersToProcess:List[TickerLoadType] = state.tickers diff state.loadedTickers
        ticketJobService.loadTickers(jobId, tickersToProcess, state.from, state.to)
        Behaviors.same
    }
  }
}

package serv1.rest.loaddata

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.model.ticker.TickerLoadType

import java.time.LocalDateTime
import java.util.UUID

object LoadDataActor {
  case class LoadPeriod(from: LocalDateTime, to: LocalDateTime)

  case class LoadDataRequest(tickers: List[TickerLoadType], period: LoadPeriod)

  case class ReloadDataRequest(tickers: List[TickerLoadType])

  sealed trait ResponseMessage

  case class LoadDataResponse(job: UUID) extends ResponseMessage

  case class LoadDataResponses(jobs: Seq[UUID]) extends ResponseMessage

  sealed trait Message

  // LoadDataRequest loads tickers bars, skipping existing data
  case class LoadDataRequestRef(loadDataRequest: LoadDataRequest, replyTo: ActorRef[LoadDataResponse]) extends Message

  // ReloadDataRequest loads tickers bars rewriting existing data
  case class ReloadDataRequestRef(reloadDataRequest: ReloadDataRequest, replyTo: ActorRef[LoadDataResponses]) extends Message

  def apply(loadService: LoadService): Behavior[Message] = {
    Behaviors.receive {
      case (_, LoadDataRequestRef(loadDataRequest, replyTo)) =>
        val request = loadDataRequest
        replyTo ! LoadDataResponse(loadService.load(request.tickers,
          request.period.from,
          request.period.to, overwrite = false))
        Behaviors.same
      case (_, ReloadDataRequestRef(reloadDataRequest, replyTo)) =>
        val request = reloadDataRequest
        replyTo ! LoadDataResponses(loadService.load(request.tickers))
        Behaviors.same
    }
  }
}
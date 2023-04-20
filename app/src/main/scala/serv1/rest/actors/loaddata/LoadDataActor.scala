package serv1.rest.actors.loaddata

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.model.ticker.TickerLoadType
import serv1.rest.services.loaddata.LoadService

import java.time.LocalDateTime
import java.util.UUID

object LoadDataActor {
  case class LoadPeriod(from: LocalDateTime, to: LocalDateTime)

  case class LoadDataRequest(tickers: List[TickerLoadType], period: LoadPeriod)

  case class StartLoadingTickDataRequest(ticker: TickerLoadType)

  case class StopLoadingTickDataRequest(tickers: List[TickerLoadType]) extends Message

  case class ReloadDataRequest(tickers: List[TickerLoadType])

  sealed trait ResponseMessage

  case class LoadDataResponse(job: UUID) extends ResponseMessage

  case class LoadDataResponses(jobs: Seq[UUID]) extends ResponseMessage

  case class StartLoadingTickDataResponse(jod: UUID) extends ResponseMessage

  case class StopLoadingTickDataResponse(notFound: Boolean) extends ResponseMessage

  sealed trait Message

  // LoadDataRequest loads tickers bars, skipping existing data
  case class LoadDataRequestRef(loadDataRequest: LoadDataRequest, replyTo: ActorRef[ResponseMessage]) extends Message

  // ReloadDataRequest loads tickers bars rewriting existing data
  case class ReloadDataRequestRef(reloadDataRequest: ReloadDataRequest, replyTo: ActorRef[ResponseMessage]) extends Message

  case class StartLoadingTickDataRequestRef(startLoadingTickDataRequest: StartLoadingTickDataRequest, replyTo: ActorRef[ResponseMessage]) extends Message

  case class StopLoadingTickDataRequestRef(stopLoadingTickDataRequest: StopLoadingTickDataRequest, replyTo: ActorRef[ResponseMessage]) extends Message


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
      case (_, StartLoadingTickDataRequestRef(startLoadingTickDataRequest, replyTo)) =>
        replyTo ! StartLoadingTickDataResponse(loadService.startTickLoad(Seq(startLoadingTickDataRequest.ticker)).head)
        Behaviors.same
      case (_, StopLoadingTickDataRequestRef(stopLoadingTickDataRequest, replyTo)) =>
        replyTo ! StopLoadingTickDataResponse(loadService.stopTickLoad(stopLoadingTickDataRequest.tickers))
        Behaviors.same
    }
  }
}
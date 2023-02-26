package serv1.rest.loaddata

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.model.ticker.TickerLoadType

import java.time.LocalDateTime
import java.util.UUID

object LoadDataActor {
  case class LoadPeriod(from: LocalDateTime, to: LocalDateTime)

  case class LoadDataRequest(tickers: List[TickerLoadType], period: LoadPeriod)

  case class LoadDataResponse(job: UUID)

  case class LoadDataRequestRef(loadDataRequest: LoadDataRequest, replyTo: ActorRef[LoadDataResponse])

  def apply(loadService: LoadService): Behavior[LoadDataRequestRef] = {
    Behaviors.receive { (_, data) =>
      val request = data.loadDataRequest
      data.replyTo ! LoadDataResponse(loadService.load(request.tickers,
        request.period.from,
        request.period.to))
      Behaviors.same
    }
  }
}
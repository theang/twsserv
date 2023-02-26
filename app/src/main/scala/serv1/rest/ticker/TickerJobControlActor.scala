package serv1.rest.ticker

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.db.repo.intf.{ScheduledTaskRepoIntf, TickerTrackingRepoIntf, TickerTypeRepoIntf}
import serv1.model.ticker.TickerLoadType

object TickerJobControlActor {
  case class AddTickersTrackingRequest(scheduleName: String, tickers: List[TickerLoadType])

  case class RemoveTickersTrackingRequest(scheduleName: String, tickers: List[TickerLoadType])

  case class GetStatusRequest(scheduleName: String)

  sealed trait RequestMessage

  case class AddTickersTrackingRequestRef(addTickersTrackingRequest: AddTickersTrackingRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class RemoveTickersTrackingRequestRef(removeTickersTrackingRequest: RemoveTickersTrackingRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class GetStatusRequestRef(getStatusRequest: GetStatusRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  sealed trait ResponseMessage

  case class TickersTrackingResponse(scheduleName: String, tickers: List[TickerLoadType]) extends ResponseMessage

  def resolveIds(tickerTypeRepoIntf: TickerTypeRepoIntf, tickerTypes: List[TickerLoadType]): Seq[Int] = {
    tickerTypes.map(tickerTypeRepoIntf.queryTickerType)
  }

  def apply(scheduledTaskRepoIntf: ScheduledTaskRepoIntf, tickerTrackingRepoIntf: TickerTrackingRepoIntf, tickerTypeRepoIntf: TickerTypeRepoIntf): Behavior[RequestMessage] = {
    Behaviors.receiveMessage {
      case AddTickersTrackingRequestRef(addTickersTrackingRequest, replyTo) =>
        val ids = resolveIds(tickerTypeRepoIntf, addTickersTrackingRequest.tickers)
        val scheduleName = addTickersTrackingRequest.scheduleName
        val scheduleId = scheduledTaskRepoIntf.getScheduledTaskByName(scheduleName).head.id
        tickerTrackingRepoIntf.addTickersTracking(scheduleId, ids)
        val updatedTickers = tickerTypeRepoIntf.queryTickers(
          tickerTrackingRepoIntf.findTickerTracking(scheduleId))
        replyTo ! TickersTrackingResponse(scheduleName, updatedTickers.toList)
        Behaviors.same
      case RemoveTickersTrackingRequestRef(removeTickersTrackingRequest, replyTo) =>
        val ids = resolveIds(tickerTypeRepoIntf, removeTickersTrackingRequest.tickers)
        val scheduleName = removeTickersTrackingRequest.scheduleName
        val scheduleId = scheduledTaskRepoIntf.getScheduledTaskByName(scheduleName).head.id
        tickerTrackingRepoIntf.removeTickersTracking(scheduleId, ids)
        val updatedTickers = tickerTypeRepoIntf.queryTickers(
          tickerTrackingRepoIntf.findTickerTracking(scheduleId))
        replyTo ! TickersTrackingResponse(scheduleName, updatedTickers.toList)
        Behaviors.same
      case GetStatusRequestRef(getStatusRequest, replyTo) =>
        val scheduleName = getStatusRequest.scheduleName
        val scheduleId = scheduledTaskRepoIntf.getScheduledTaskByName(scheduleName).head.id
        val tickers = tickerTypeRepoIntf.queryTickers(
          tickerTrackingRepoIntf.findTickerTracking(scheduleId))
        replyTo ! TickersTrackingResponse(scheduleName, tickers.toList)
        Behaviors.same
    }
  }
}

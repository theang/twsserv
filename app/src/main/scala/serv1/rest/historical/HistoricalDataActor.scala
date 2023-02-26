package serv1.rest.historical

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.db.repo.intf.TickerDataRepoIntf
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import serv1.rest.loaddata.LoadDataActor.LoadPeriod
import serv1.util.LocalDateTimeUtil

object HistoricalDataActor {
  case class HistoricalDataResponse(tickers: Map[TickerLoadType, Seq[HistoricalData]])

  case class HistoricalDataRequest(tickers: List[TickerLoadType], period: LoadPeriod, replyTo: ActorRef[HistoricalDataResponse])

  def apply(tickerDataRepo: TickerDataRepoIntf): Behavior[HistoricalDataRequest] = {
    Behaviors.receiveMessage[HistoricalDataRequest] {
      case HistoricalDataRequest(tickers, period, replyTo) =>
        val resultMap = tickers.map(ticker => (ticker, tickerDataRepo.readRange(ticker,
          LocalDateTimeUtil.toEpoch(period.from),
          LocalDateTimeUtil.toEpoch(period.to)))).toMap
        replyTo ! HistoricalDataResponse(resultMap)
        Behaviors.same
    }
  }
}

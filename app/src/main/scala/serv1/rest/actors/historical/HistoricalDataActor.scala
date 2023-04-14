package serv1.rest.actors.historical

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.db.repo.intf.TickerDataRepoIntf
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import serv1.rest.actors.loaddata.LoadDataActor.LoadPeriod
import serv1.util.LocalDateTimeUtil

object HistoricalDataActor {
  case class HistoricalDataValues(ticker: TickerLoadType, data: Seq[HistoricalData])

  case class HistoricalDataResponse(tickers: Seq[HistoricalDataValues])

  case class HistoricalDataRequest(tickers: List[TickerLoadType], period: LoadPeriod, replyTo: ActorRef[HistoricalDataResponse])

  def apply(tickerDataRepo: TickerDataRepoIntf): Behavior[HistoricalDataRequest] = {
    Behaviors.receiveMessage[HistoricalDataRequest] {
      case HistoricalDataRequest(tickers, period, replyTo) =>
        val resultList = tickers.map(ticker => HistoricalDataValues(ticker, tickerDataRepo.readRange(ticker,
          LocalDateTimeUtil.toEpoch(period.from),
          LocalDateTimeUtil.toEpoch(period.to))))
        replyTo ! HistoricalDataResponse(resultList)
        Behaviors.same
    }
  }
}

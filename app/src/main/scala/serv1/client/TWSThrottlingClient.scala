package serv1.client

import akka.actor.typed.ActorRef
import serv1.client.DataClientThrottlingActor.{LoadHistoricalData, Message}
import serv1.client.operations.ClientOperationCallbacks.{HistoricalDataOperationCallback, TickBidAskOperationCallback, TickLastOperationCallback}
import serv1.client.operations.ClientOperationHandlers.ErrorHandler
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.ticker.TickerType
import slick.util.Logging

class TWSThrottlingClient(throttlingActor: ActorRef[Message]) extends DataClient with Logging {

  override def loadHistoricalData(from: Long, to: Long, tickerType: TickerType, barSize: Int, historicalDataType: HistoricalDataType, cont: HistoricalDataOperationCallback, error: ErrorHandler): Unit = {
    throttlingActor ! LoadHistoricalData(from, to, tickerType, barSize, historicalDataType, cont, error)
  }

  override def startLoadingTickData(tickerType: TickerType, contLast: TickLastOperationCallback, contBidAsk: TickBidAskOperationCallback, error: ErrorHandler): (Int, Int) = {
    TWSClient.startLoadingTickData(tickerType, contLast, contBidAsk, error)
  }

  override def cancelLoadingTickData(reqLastN: Int, reqBidAskN: Int): Unit = {
    TWSClient.cancelLoadingTickData(reqLastN, reqBidAskN)
  }
}

package serv1.client

import serv1.client.model.Earnings
import serv1.client.operations.{ClientOperationCallbacks, ClientOperationHandlers}
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.ticker.TickerType

trait DataClient {
  def loadHistoricalData(from: Long, to: Long, tickerType: TickerType, barSize: Int,
                         historicalDataType: HistoricalDataType,
                         cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                         error: ClientOperationHandlers.ErrorHandler): Unit

  def startLoadingTickData(tickerType: TickerType,
                           contLast: ClientOperationCallbacks.TickLastOperationCallback,
                           contBidAsk: ClientOperationCallbacks.TickBidAskOperationCallback,
                           error: ClientOperationHandlers.ErrorHandler): (Int, Int)

  def cancelLoadingTickData(reqLastN: Int, reqBidAskN: Int): Unit

  def getEarningsForDate(date: Long): Seq[Earnings] = Seq.empty
}

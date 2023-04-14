package serv1.client

import serv1.client.operations.{ClientOperationCallbacks, ClientOperationHandlers}

trait DataClient {
  def loadHistoricalData(from: Long, to: Long, ticker: String, exchange: String, typ: String, barSize: Int, prec: Int,
                         cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                         error: ClientOperationHandlers.ErrorHandler): Unit

  def startLoadingTickData(ticker: String, exchange: String, typ: String,
                           contLast: ClientOperationCallbacks.TickLastOperationCallback,
                           contBidAsk: ClientOperationCallbacks.TickBidAskOperationCallback,
                           error: ClientOperationHandlers.ErrorHandler): (Int, Int)

  def cancelLoadingTickData(reqLastN: Int, reqBidAskN: Int): Unit
}

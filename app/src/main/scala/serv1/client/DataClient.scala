package serv1.client

import serv1.client.operations.ClientOperationHandlers

trait DataClient {
  def loadHistoricalData(from: Long, to: Long, ticker: String, exchange: String, typ: String, barSize: Int, prec: Int,
                         cont: ClientOperationHandlers.HistoricalDataOperationCallback,
                         error: ClientOperationHandlers.ErrorHandler): Unit
}

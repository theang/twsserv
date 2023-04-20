package serv1.client

import com.typesafe.config.Config
import serv1.client.operations.ClientOperationCallbacks
import serv1.client.operations.ClientOperationCallbacks.{TickBidAskOperationCallback, TickLastOperationCallback}
import serv1.client.operations.ClientOperationHandlers.ErrorHandler
import serv1.config.ServConfig
import serv1.model.ticker.TickerType
import slick.util.Logging

object MultiClient extends DataClient {
  val clients: Map[String, DataClient with Logging] = Map("TWSClient" -> TWSClient,
    "YahooClient" -> YahooClient)
  var config: Config = ServConfig.config.getConfig("multiClient")
  var currentClient: String = config.getString("defaultClient")

  override def loadHistoricalData(from: Long,
                                  to: Long,
                                  tickerType: TickerType,
                                  barSize: Int,
                                  cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                                  error: ErrorHandler): Unit =
    clients(currentClient).loadHistoricalData(from, to, tickerType, barSize, cont, error)

  override def startLoadingTickData(tickerType: TickerType, contLast: TickLastOperationCallback, contBidAsk: TickBidAskOperationCallback, error: ErrorHandler): (Int, Int) = {
    clients(currentClient).startLoadingTickData(tickerType, contLast, contBidAsk, error)
  }

  override def cancelLoadingTickData(reqLastN: Int, reqBidAskN: Int): Unit = {
    clients(currentClient).cancelLoadingTickData(reqLastN, reqBidAskN)
  }
}
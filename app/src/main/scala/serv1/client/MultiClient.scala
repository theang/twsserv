package serv1.client

import com.typesafe.config.Config
import serv1.client.model.Earnings
import serv1.client.operations.ClientOperationCallbacks
import serv1.client.operations.ClientOperationCallbacks.{TickBidAskOperationCallback, TickLastOperationCallback}
import serv1.client.operations.ClientOperationHandlers.ErrorHandler
import serv1.config.ServConfig
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.ticker.TickerType
import slick.util.Logging

import scala.jdk.CollectionConverters._
object MultiClient extends DataClient {
  val clients: Map[String, DataClient with Logging] = Map("TWSClient" -> TWSClient,
    "YahooClient" -> YahooClient, "NasdaqClient" -> NasdaqClient)
  var config: Config = ServConfig.config.getConfig("multiClient")
  var currentClient: String = config.getString("defaultClient")
  var clientsMap: Map[String, String] = config.getConfigList("clientsMap").asScala.map {
    config: Config => (config.getString("method"), config.getString("client"))
  }.toMap

  def getClient(method: String): DataClient = {
    clients(clientsMap.getOrElse(method, currentClient))
  }

  override def loadHistoricalData(from: Long,
                                  to: Long,
                                  tickerType: TickerType,
                                  barSize: Int,
                                  historicalDataType: HistoricalDataType,
                                  cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                                  error: ErrorHandler): Unit =
    getClient("loadHistoricalData").loadHistoricalData(from, to, tickerType, barSize, historicalDataType, cont, error)

  override def startLoadingTickData(tickerType: TickerType, contLast: TickLastOperationCallback, contBidAsk: TickBidAskOperationCallback, error: ErrorHandler): (Int, Int) = {
    getClient("startLoadingTickData").startLoadingTickData(tickerType, contLast, contBidAsk, error)
  }

  override def cancelLoadingTickData(reqLastN: Int, reqBidAskN: Int): Unit = {
    getClient("cancelLoadingTickData").cancelLoadingTickData(reqLastN, reqBidAskN)
  }

  override def getEarningsForDate(date: Long): Seq[Earnings] = {
    getClient("getEarningsForDate").getEarningsForDate(date)
  }
}
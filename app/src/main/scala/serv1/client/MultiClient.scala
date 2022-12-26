package serv1.client
import com.typesafe.config.Config
import serv1.client.operations.ClientOperationHandlers.{ErrorHandler, HistoricalDataOperationCallback}
import serv1.config.ServConfig
import slick.util.Logging

object MultiClient extends DataClient {
  val clients: Map[String, DataClient with Logging] = Map("TWSClient" -> TWSClient,
    "YahooClient" -> YahooClient)
  var config: Config = ServConfig.config.getConfig("multiClient")
  var currentClient: String = config.getString("defaultClient")

  override def loadHistoricalData(from: Long,
                                  to: Long,
                                  ticker: String,
                                  exchange: String,
                                  typ: String,
                                  barSize: Int,
                                  prec: Int,
                                  cont: HistoricalDataOperationCallback,
                                  error: ErrorHandler): Unit =
    clients(currentClient).loadHistoricalData(from, to, ticker, exchange, typ, barSize, prec, cont, error)
}
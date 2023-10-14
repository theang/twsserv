package serv1.client

import com.typesafe.config.Config
import serv1.client.exception.ClientException
import serv1.client.model.{Earnings, NasdaqJsonFormats}
import serv1.client.operations.ClientOperationCallbacks
import serv1.client.operations.ClientOperationHandlers.ErrorHandler
import serv1.config.ServConfig
import serv1.model.ticker.TickerType
import serv1.time.HighResTime
import serv1.util.{LocalDateTimeUtil, PowerOperator}
import slick.util.Logging
import spray.json._

import java.io.InputStream
import java.net.{HttpURLConnection, URL}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.zip.GZIPInputStream

object NasdaqClient extends DataClient with Logging with PowerOperator with NasdaqJsonFormats {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  var config: Config = ServConfig.config.getConfig("nasdaqClient")
  var earningsUrl: String = config.getString("earningsUrl")
  val nasdaqKey: String = config.getString("nasdaqKey")
  val rateLimitRequests: Int = config.getInt("rateLimitRequests")
  val rateLimitTimeSecond: Int = config.getInt("rateLimitTimeSecond")
  val timeBetweenRequestsMs: Double = rateLimitTimeSecond.toDouble * 1000 / rateLimitRequests
  val contentEncodingHeader: String = "Content-Encoding"
  val gzip = "gzip"
  var lastRequest: Long = 0

  def startRequest(): Unit = {
    lastRequest = HighResTime.currentNanos
  }

  def delayTillNextRequest(): Long = {
    if (lastRequest == 0) {
      return 0
    }
    val curNanos = HighResTime.currentNanos
    val msPassed = (curNanos - lastRequest) / 1_000_000
    val msToNextRequest = Math.round(timeBetweenRequestsMs - msPassed)
    Math.max(msToNextRequest, 0)
  }

  def formatDate(date: String): Long = {
    LocalDateTimeUtil.toEpoch(LocalDate.parse(date, formatter).atStartOfDay)
  }

  def epochToDate(ts: Long): LocalDateTime = {
    LocalDateTimeUtil.fromEpoch(ts)
  }

  override def loadHistoricalData(from: Long, to: Long, tickerType: TickerType, barSize: Int,
                                  cont: ClientOperationCallbacks.HistoricalDataOperationCallback, error: ErrorHandler): Unit = ???

  override def startLoadingTickData(tickerType: TickerType,
                                    contLast: ClientOperationCallbacks.TickLastOperationCallback,
                                    contBidAsk: ClientOperationCallbacks.TickBidAskOperationCallback, error: ErrorHandler): (Int, Int) = ???

  override def cancelLoadingTickData(reqLastN: Int, reqBidAskN: Int): Unit = ???

  def setDefaultProperties(connection: HttpURLConnection): Unit = {
    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
    connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br")
    connection.setRequestProperty("User-Agent", "Java-http-client/")
  }

  def parseEarningsData(stream: InputStream): Seq[Earnings] = {
    val parserInput = ParserInput(stream.readAllBytes())
    val parser = JsonParser(parserInput)
    val earnings = parser.asJsObject.fields("data").asJsObject.fields("rows")
    earnings match {
      case JsArray(v) => v.map(_.convertTo[Earnings])
      case _ => Seq.empty
    }
  }

  override def getEarningsForDate(date: Long): Seq[Earnings] = {
    Thread.sleep(delayTillNextRequest())
    startRequest()
    val urlStr: String = earningsUrl.format(LocalDateTimeUtil.fromEpoch(date).format(formatter), nasdaqKey)
    logger.debug(s"URL: $urlStr")
    val url: URL = new URL(urlStr)
    logger.debug(s"Opening connection")
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    logger.debug(s"Setting method to GET")
    connection.setRequestMethod("GET")
    logger.debug(s"Connecting")
    setDefaultProperties(connection)

    connection.connect()
    if (connection.getResponseCode >= 400) {
      logger.debug(s"ResponseCode: ${connection.getResponseCode}")
      throw new ClientException()
    } else {
      logger.debug(s"reading Earnings data")
      val isGzip = connection.getHeaderField(contentEncodingHeader) != null && connection.getHeaderField(contentEncodingHeader).equals(gzip)
      val stream = if (isGzip) new GZIPInputStream(connection.getInputStream) else connection.getInputStream
      parseEarningsData(stream)
    }
  }

  def main(args: Array[String]): Unit = {
    val date = args(0)
    val earnings = NasdaqClient.getEarningsForDate(LocalDateTimeUtil.toEpoch(LocalDate.parse(date, formatter).atStartOfDay()))
    earnings.foreach(println(_))
  }
}

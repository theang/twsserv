package serv1.client

import com.typesafe.config.Config
import serv1.client.converters.BarSizeConverter
import serv1.client.operations.ClientOperationCallbacks
import serv1.client.operations.ClientOperationHandlers.ErrorHandler
import serv1.config.ServConfig
import serv1.model.HistoricalData
import serv1.model.ticker.TickerType
import serv1.util.{LocalDateTimeUtil, PowerOperator}
import slick.util.Logging

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.{HttpURLConnection, URL}
import java.time.LocalDate
import java.time.format.{DateTimeFormatter, DateTimeParseException}

object YahooClient extends DataClient with Logging with PowerOperator {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  var config: Config = ServConfig.config.getConfig("yahooClient")
  var baseUrl: String = config.getString("baseUrl")

  def formatDate(date: String): Long = {
    LocalDateTimeUtil.toEpoch(LocalDate.parse(date, formatter).atStartOfDay)
  }

  def getBarSize(barSizeSec: Int): String = {
    //1m2m5m15m30m60m90m1h1d5d1wk1mo3mo
    if (barSizeSec < BarSizeConverter.hr) {
      s"${barSizeSec / 60}m"
    } else if (barSizeSec < BarSizeConverter.day) {
      "1h"
    } else {
      barSizeSec match {
        case BarSizeConverter.day => "1d"
        case s if s == (5 * BarSizeConverter.day) => "5d"
        case s if s == (7 * BarSizeConverter.day) => "1wk"
        case s if s >= (28 * BarSizeConverter.day) && s <= (31 * BarSizeConverter.day) => "1mo"
        case s if s >= (89 * BarSizeConverter.day) && s <= (92 * BarSizeConverter.day) => "3mo"
        case _ => "1d"
      }
    }
  }

  def parseHistoricalData(string: String, prec: Int): HistoricalData = {
    string.split(",").toVector.map(_.trim) match {
      case Vector(date, open, high, low, close, adjClose, vol) =>
        try {
          HistoricalData(formatDate(date),
            (high.toFloat * (10 ** prec)).toLong,
            (low.toFloat * (10 ** prec)).toLong,
            (open.toFloat * (10 ** prec)).toLong,
            (close.toFloat * (10 ** prec)).toLong,
            vol.toDouble)
        } catch {
          case exc: DateTimeParseException =>
            logger.warn(s"Could not parse date: $date")
            null
        }
      case _ =>
        logger.warn(s"Invalid format: $string")
        null
    }
  }

  def readHistoricalDataFromStream(inpStr: InputStream, prec: Int): Seq[HistoricalData] = {
    val br: BufferedReader = new BufferedReader(new InputStreamReader(inpStr))
    LazyList.continually(br.readLine()).takeWhile {
      _ != null
    }.map(it => parseHistoricalData(it, prec)).filter {
      _ != null
    }
  }

  override def loadHistoricalData(from: Long, to: Long, tickerType: TickerType, barSize: Int,
                                  cont: ClientOperationCallbacks.HistoricalDataOperationCallback, error: ErrorHandler): Unit = {
    val urlStr: String = baseUrl.format(tickerType.name,
      from.toString,
      to.toString,
      getBarSize(barSize))
    logger.debug(s"URL: $urlStr")
    val url: URL = new URL(urlStr)
    logger.debug(s"Opening connection")
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    logger.debug(s"Setting method to GET")
    connection.setRequestMethod("GET")
    logger.debug(s"Connecting")
    connection.connect()
    if (connection.getResponseCode >= 400) {
      logger.debug(s"ResponseCode: ${connection.getResponseCode}")
      error(connection.getResponseCode, "Error when loading historical data", "")
    } else {
      logger.debug(s"reading Historical Data")
      cont(readHistoricalDataFromStream(connection.getInputStream, tickerType.prec), true)
    }
  }

  override def startLoadingTickData(tickerType: TickerType,
                                    contLast: ClientOperationCallbacks.TickLastOperationCallback,
                                    contBidAsk: ClientOperationCallbacks.TickBidAskOperationCallback, error: ErrorHandler): (Int, Int) = ???

  override def cancelLoadingTickData(reqLastN: Int, reqBidAskN: Int): Unit = ???
}

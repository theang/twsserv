package serv1.db.schema

import serv1.db.Configuration
import serv1.db.types.HistoricalDataType
import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}

import scala.util.matching.Regex

object TickerDataTableNameUtil {

  def futureDate(tt: TickerLoadType): String = tt.tickerType.lastTradeDateOrContractMonth.map { ld => s"_$ld" }.getOrElse("")

  def formatTableName(tt: TickerLoadType): String = s"TD_${tt.tickerType.exchange}_${tt.tickerType.typ}_${tt.tickerType.name}_${tt.barSize}${futureDate(tt)}"

  def formatTickTableName(tt: TickerLoadType, tickType: String): String =
    s"TD_${tt.tickerType.exchange}_${tt.tickerType.typ}_${tt.tickerType.name}_${tt.barSize}${futureDate(tt)}_$tickType"

  val tableNameRegex: Regex = "^TD_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)(?:_([a-zA-Z0-9]+))?$".r

  val tickTableNameRegex: Regex = "^TD_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)(?:_([a-zA-Z0-9]+))?_(LAST|BIDASK)$".r

  def parseTableName(name: String): Option[TickerLoadType] = name match {
    case tableNameRegex(exchange, typ, name, barSizeStr, futureDateStr) =>
      try {
        val barSize = BarSizes.withName(barSizeStr)
        Some(TickerLoadType(TickerType(name, exchange, typ, Configuration.defaultPrecision, Option.empty, Option.empty, Option.empty, Option.empty, Option(futureDateStr),
          Option.empty, Option.empty), barSize, HistoricalDataType.TRADES))
      } catch {
        case _: NoSuchElementException =>
          None
      }
    case _ => None
  }
}

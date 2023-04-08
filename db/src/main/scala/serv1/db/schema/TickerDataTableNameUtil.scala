package serv1.db.schema

import serv1.db.Configuration
import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}

import scala.util.matching.Regex

object TickerDataTableNameUtil {
  def formatTableName(tt: TickerLoadType): String = s"TD_${tt.tickerType.exchange}_${tt.tickerType.typ}_${tt.tickerType.name}_${tt.barSize}"

  val tableNameRegex: Regex = "^TD_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)_([a-zA-Z0-9]+)$".r

  def parseTableName(name: String): Option[TickerLoadType] = name match {
    case tableNameRegex(exchange, typ, name, barSizeStr) =>
      try {
        val barSize = BarSizes.withName(barSizeStr)
        Some(TickerLoadType(TickerType(name, exchange, typ, Configuration.defaultPrecision), barSize))
      } catch {
        case _: NoSuchElementException =>
          None
      }
    case _ => None
  }
}

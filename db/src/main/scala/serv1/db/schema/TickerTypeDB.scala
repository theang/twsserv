package serv1.db.schema

import serv1.model.ticker.BarSizes.BarSize
import serv1.model.ticker.{TickerLoadType, TickerType}

import scala.language.implicitConversions

case class TickerTypeDB(id: Int, name: String, exchange: String, typ: String, barSize: BarSize, prec: Int,
                        localSymbol: Option[String], strike: Option[Int], right: Option[String], multiplier: Option[String],
                        lastTradeDateOrContractMonth: Option[String], currency: Option[String], primaryExchange: Option[String])

object TickerTypeDB {
  implicit def tickerLoadTypeToTickerTypeDB(tickerLoadType: TickerLoadType): TickerTypeDB = {
    TickerTypeDB(0, tickerLoadType.tickerType.name, tickerLoadType.tickerType.exchange, tickerLoadType.tickerType.typ, tickerLoadType.barSize, tickerLoadType.tickerType.prec,
      tickerLoadType.tickerType.localSymbol, tickerLoadType.tickerType.strike, tickerLoadType.tickerType.right, tickerLoadType.tickerType.multiplier,
      tickerLoadType.tickerType.lastTradeDateOrContractMonth, tickerLoadType.tickerType.currency, tickerLoadType.tickerType.primaryExchange)
  }

  implicit def tickerTypeDBToTickerLoadType(tickerTypeDB: TickerTypeDB): TickerLoadType = {
    TickerLoadType(TickerType(tickerTypeDB.name, tickerTypeDB.exchange, tickerTypeDB.typ, tickerTypeDB.prec, tickerTypeDB.localSymbol,
      tickerTypeDB.strike, tickerTypeDB.right, tickerTypeDB.multiplier, tickerTypeDB.lastTradeDateOrContractMonth, tickerTypeDB.currency, tickerTypeDB.primaryExchange), tickerTypeDB.barSize)
  }
}

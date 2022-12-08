package serv1.db.schema

import serv1.model.ticker.BarSizes.BarSize
import serv1.model.ticker.{TickerLoadType, TickerType}

import scala.language.implicitConversions

case class TickerTypeDB(id: Int, name: String, exchange: String, typ: String, barSize: BarSize, prec: Int)

object TickerTypeDB {
  implicit def tickerLoadTypeToTickerTypeDB(tickerLoadType: TickerLoadType): TickerTypeDB = {
    TickerTypeDB(0, tickerLoadType.tickerType.name, tickerLoadType.tickerType.exchange, tickerLoadType.tickerType.typ, tickerLoadType.barSize, tickerLoadType.tickerType.prec)
  }

  implicit def tickerTypeDBToTickerLoadType(tickerTypeDB: TickerTypeDB): TickerLoadType = {
    TickerLoadType(TickerType(tickerTypeDB.name, tickerTypeDB.exchange, tickerTypeDB.typ, tickerTypeDB.prec), tickerTypeDB.barSize)
  }
}

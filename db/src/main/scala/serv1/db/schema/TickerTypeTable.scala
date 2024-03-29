package serv1.db.schema

import serv1.model.ticker.{BarSizes, TickerLoadType}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object TickerTypeTable {
  val query = TableQuery[TickerTypeTable]

  def findTicker(tt: TickerLoadType): TickerTypeTable => Rep[Boolean] = (e: TickerTypeTable) => e.name === tt.tickerType.name && e.exchange === tt.tickerType.exchange && e.typ === tt.tickerType.typ && e.barSize === tt.barSize
}

class TickerTypeTable(tag: Tag) extends Table[TickerTypeDB](tag, "TICKER") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

  def name = column[String]("NAME")

  def exchange = column[String]("EXCHANGE")

  def typ = column[String]("TYP")

  def barSize: Rep[BarSizes.Value] = column[BarSizes.Value]("BAR_SIZE")

  def prec = column[Int]("PREC")

  def localSymbol = column[Option[String]]("LOCAL_SYMBOL")

  def strike = column[Option[Int]]("STRIKE")

  def right = column[Option[String]]("RIGHT")

  def multiplier = column[Option[String]]("MULTIPLIER")

  def lastTradeDateOrContractMonth = column[Option[String]]("LAST_TRADE_DATE_OR_CONTRACT_MONTH")

  def currency = column[Option[String]]("CURRENCY")

  def primaryExchange = column[Option[String]]("PRIMARY_EXCHANGE")

  def * = (id, name, exchange, typ, barSize, prec, localSymbol, strike, right, multiplier, lastTradeDateOrContractMonth, currency, primaryExchange) <> ((TickerTypeDB.apply _).tupled, TickerTypeDB.unapply)

  def nameIndex = index("IND_NAME", (name, exchange, typ, barSize, prec, localSymbol, strike, right, multiplier, lastTradeDateOrContractMonth, currency, primaryExchange), unique = true)
}

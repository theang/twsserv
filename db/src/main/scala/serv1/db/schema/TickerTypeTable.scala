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

  def * = (id, name, exchange, typ, barSize, prec) <> ((TickerTypeDB.apply _).tupled, TickerTypeDB.unapply)

  def nameIndex = index("IND_NAME", (name, exchange, typ, barSize, prec), unique = true)
}

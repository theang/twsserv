package serv1.db.schema

import serv1.db.DB
import serv1.model.ticker.BarSizes
import serv1.model.ticker.BarSizes.BarSize
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.MappedToBase.mappedToIsomorphism
import slick.lifted.Tag

object TickerTypeTable {
  val query = TableQuery[TickerTypeTable]
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

package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object TickerDataErrorsTable {
  val query = TableQuery[TickerDataErrorsTable]
}

class TickerDataErrorsTable(tag: Tag) extends Table[TickerDataErrors](tag, "TICKER_DATA_ERRORS") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

  def error = column[String]("ERROR")

  def * = (id, error) <> (TickerDataErrors.tupled, TickerDataErrors.unapply)
}

package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object EventTable {
  def query = TableQuery[EventTable]
}

class EventTable(tag: Tag) extends Table[Event](tag, _tableName = "EVENT") {
  def id = column[Int]("ID", O.PrimaryKey, O.Unique, O.AutoInc)

  def typ = column[String]("TYP")

  def symbol = column[String]("SYMBOL")

  def time = column[Long]("TIME")

  def info = column[String]("INFO")

  def * = (id, typ, symbol, time, info) <> (Event.tupled, Event.unapply)

  def indexTypTime = index("IND_EVENT_TYP_TIME", (typ, time))

  def indexTypSymbolTime = index("IND_EVENT_TYP_SYMBOL_TIME", (typ, symbol, time), unique = true)
  def indexTime = index("IND_EVENT_TIME", time)
}
package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}

object ExchangeTable {
  def query = TableQuery[ExchangeTable]
}

class ExchangeTable(tag: Tag) extends Table[Exchange](tag, "EXCHANGE") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

  def name = column[String]("NAME")

  override def * : ProvenShape[Exchange] = (id, name) <> (Exchange.tupled, Exchange.unapply)

  def indexName = index("IND_EXCHANGE_TABLE_NAME", name, unique = true)
}

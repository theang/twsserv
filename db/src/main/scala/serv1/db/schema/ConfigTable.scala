package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object ConfigTable {
  val query = TableQuery[ConfigTable]
}

class ConfigTable(tag: Tag) extends Table[Config](tag, "CONFIG") {
  def typ = column[String]("TYP")

  def name = column[String]("NAME")

  def value = column[String]("VALUE")

  def * = (typ, name, value) <> (Config.tupled, Config.unapply)

  def indexTypName = index("IND_CONFIG_TABLE_TYP_NAME", (typ, name), unique = true)
}
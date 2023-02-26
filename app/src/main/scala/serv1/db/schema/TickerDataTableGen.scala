package serv1.db.schema

import serv1.model.ticker.TickerLoadType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object TickerDataTable {
  def getTable(tickerLoadType: TickerLoadType): TickerDataTableGen = {
    new TickerDataTableGen(tickerLoadType)
  }

  def getQuery(tickerLoadType: TickerLoadType): TableQuery[_ <: Table[TickerData]] = {
    val tickerDataTableGen: TickerDataTableGen = getTable(tickerLoadType)
    TableQuery[tickerDataTableGen.TickerDataTable]
  }
}

class TickerDataTableGen(tt: TickerLoadType) {

  val tableName: String = TickerDataTableNameUtil.formatTableName(tt)

  class TickerDataTable(tag: Tag) extends Table[TickerData](tag, tableName) {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def time = column[Long]("TIME")

    def open = column[Long]("OPEN")

    def high = column[Long]("HIGH")

    def low = column[Long]("LOW")

    def close = column[Long]("CLOSE")

    def volume = column[Double]("VOL")

    def * = (id, time, open, high, low, close, volume) <> ((TickerData.apply _).tupled, TickerData.unapply)

    def timeIndex = index(s"IND_TIME_$tableName", time, unique = true)
  }
}

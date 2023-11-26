package serv1.db.schema

import serv1.db.types.HistoricalDataType
import serv1.db.types.HistoricalDataType.HistoricalDataType
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

    def open = column[Double]("OPEN")

    def high = column[Double]("HIGH")

    def low = column[Double]("LOW")

    def close = column[Double]("CLOSE")

    def volume = column[Double]("VOL")

    def historicalDataType = column[HistoricalDataType]("TYP", O.Default(HistoricalDataType.TRADES))

    def * = (id, time, open, high, low, close, volume, historicalDataType) <> ((TickerData.apply _).tupled, TickerData.unapply)

    def typTimeIndex = index(s"IND_TYP_TIME_$tableName", (historicalDataType, time), unique = true)
  }
}

package serv1.db.schema

import serv1.model.ticker.TickerLoadType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

trait TickerTickTimed {
  def time: Long
}

trait TickerTickTimedTable[T <: TickerTickTimed] extends Table[T] {
  def time: Rep[Long]
}

object TickerTickTable {
  val tickLast = "LAST"
  val tickBidAsk = "BIDASK"

  def getTickLastTable(tickerLoadType: TickerLoadType): TickerTickLastTableGen = {
    new TickerTickLastTableGen(tickerLoadType)
  }

  def getTickLastQuery(tickerLoadType: TickerLoadType): TableQuery[_ <: TickerTickTimedTable[TickerTickLast]] = {
    val tickerTickLastTableGen: TickerTickLastTableGen = getTickLastTable(tickerLoadType)
    TableQuery[tickerTickLastTableGen.TickerTickLastTable]
  }

  def getTickBidAskTable(tickerLoadType: TickerLoadType): TickerTickBidAskTableGen = {
    new TickerTickBidAskTableGen(tickerLoadType)
  }

  def getTickBidAskQuery(tickerLoadType: TickerLoadType): TableQuery[_ <: TickerTickTimedTable[TickerTickBidAsk]] = {
    val tickerTickBidAskTableGen: TickerTickBidAskTableGen = getTickBidAskTable(tickerLoadType)
    TableQuery[tickerTickBidAskTableGen.TickerTickBidAskTable]
  }
}

class TickerTickLastTableGen(tt: TickerLoadType) {

  val tableName: String = TickerDataTableNameUtil.formatTickTableName(tt, TickerTickTable.tickLast)

  class TickerTickLastTable(tag: Tag) extends Table[TickerTickLast](tag, tableName) with TickerTickTimedTable[TickerTickLast] {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def time = column[Long]("TIME")

    def price = column[Double]("PRICE")

    def size = column[Double]("SIZE")

    def exch = column[Int]("EXCH")

    def spec = column[String]("SPEC", O.SqlType("VARCHAR(2)"))

    def pastLimit = column[Boolean]("PAST_LIMIT")

    def unreported = column[Boolean]("UNREPORTED")

    def * = (id, time, price, size, exch, spec, pastLimit, unreported) <> (TickerTickLast.tupled, TickerTickLast.unapply)

    def timeIndex = index(s"IND_TIME_$tableName", time, unique = false)
  }
}


class TickerTickBidAskTableGen(tt: TickerLoadType) {

  val tableName: String = TickerDataTableNameUtil.formatTickTableName(tt, TickerTickTable.tickBidAsk)

  class TickerTickBidAskTable(tag: Tag) extends Table[TickerTickBidAsk](tag, tableName) with TickerTickTimedTable[TickerTickBidAsk] {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def time = column[Long]("TIME")

    def bidPrice = column[Double]("BID_PRICE")

    def askPrice = column[Double]("ASK_PRICE")

    def bidSize = column[Double]("BID_SIZE")

    def askSize = column[Double]("ASK_SIZE")

    def bidPastLow = column[Boolean]("BID_PAST_LOW")

    def askPastHigh = column[Boolean]("ASK_PAST_HIGH")

    def * = (id, time, bidPrice, askPrice, bidSize, askSize, bidPastLow, askPastHigh) <> (TickerTickBidAsk.tupled, TickerTickBidAsk.unapply)

    def timeIndex = index(s"IND_TIME_$tableName", time, unique = false)
  }
}

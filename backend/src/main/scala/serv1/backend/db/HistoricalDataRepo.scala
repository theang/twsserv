package serv1.backend.db

import io.getquill._
import io.getquill.jdbczio.Quill
import serv1.db.schema.{TickerData, TickerDataTableNameUtil}
import serv1.db.types.HistoricalDataType
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.ticker.TickerLoadType
import zio.ZIO

import java.sql.SQLException

class HistoricalDataRepo(quill: Quill.Postgres[NamingStrategy]) {

  import quill._

  private implicit val historicalDataTypeDecoder: MappedEncoding[Int, HistoricalDataType] = MappedEncoding[Int, HistoricalDataType](
    HistoricalDataType.intToHistoricalDataTypeMap)

  private def historicalDataQuery(tickerLoadType: TickerLoadType): DynamicEntityQuery[TickerData] = {
    val tableName = TickerDataTableNameUtil.formatTableName(tickerLoadType)
    dynamicQuerySchema[TickerData](Escape.default(tableName))
  }

  private def historicalDataQ(tickerLoadType: TickerLoadType, from: Long, to: Long) = {
    historicalDataQuery(tickerLoadType).filter(record => record.time >= lift(from) && record.time <= lift(to))
  }

  private def historicalDataT(tickerLoadType: TickerLoadType, from: Long, to: Long) = {
    translate(historicalDataQ(tickerLoadType, from, to))
  }

  def historicalData(tickerLoadType: TickerLoadType, from: Long, to: Long): ZIO[Any, SQLException, List[TickerData]] = {
    run(historicalDataQ(tickerLoadType, from, to))
  }
}

object HistoricalDataRepo {
  def getHistoricalData(ticker: TickerLoadType, from: Long, to: Long): ZIO[HistoricalDataRepo, Throwable, Seq[TickerData]] = for {
    _ <- ZIO.logDebug(s"Querying data for ticker: $ticker from=$from to=$to")
    result <- ZIO.serviceWithZIO[HistoricalDataRepo](_.historicalData(ticker, from, to))
    _ <- ZIO.logDebug(s"Querying data for ticker, result: $result")
  } yield result
}
package serv1.db.repo

import serv1.Configuration
import serv1.db.DB
import serv1.db.DB.db
import serv1.db.schema.{TickerData, TickerDataTable, TickerDataTableGen, TickerDataTableNameUtil}
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import slick.dbio.DBIOAction
import slick.jdbc.meta.MTable

import java.util.concurrent.{ConcurrentHashMap, Executors}
import scala.collection.concurrent
import scala.concurrent.Await
import scala.jdk.CollectionConverters._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.Duration

object TickerDataRepo {
  val createdTables: concurrent.Map[TickerLoadType, Boolean] =
    new ConcurrentHashMap[TickerLoadType, Boolean]().asScala

  def init(): Unit = {
    val tables = Await.result(DB.db.run(MTable.getTables), Configuration.callDuration)
    tables.foreach {
      table =>
        TickerDataTableNameUtil.parseTableName(table.name.name).map {
          tt => createdTables.addOne((tt, true))
        }
    }
  }

  def createTableIfNotExists(ticker: TickerLoadType): Unit = {
    val tableQuery = TickerDataTable.getQuery(ticker)
    if (!createdTables.contains(ticker)) {
      val dbTables = Await.result(db.run(MTable.getTables), Duration.Inf).map(_.name.name)
      val tableName = TickerDataTableNameUtil.formatTableName(ticker)
      if (!dbTables.contains(tableName)) {
        Await.result(DB.db.run(DBIO.seq(tableQuery.schema.createIfNotExists)), Duration.Inf)
      }
      createdTables.addOne((ticker, true))
    }
  }

  def write(ticker: TickerLoadType, data: List[HistoricalData]): Unit = {
    createTableIfNotExists(ticker)
    val tableQuery = TickerDataTable.getQuery(ticker)
    val listToInsert = data.map({hd => TickerData(0, hd.timestamp, hd.open, hd.high, hd.low, hd.close, hd.vol)})
    Await.result(DB.db.run(DBIO.seq(tableQuery ++= listToInsert)), Duration.Inf)
  }
}

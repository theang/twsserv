package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.DB.db
import serv1.db.repo.intf.TickerDataRepoIntf
import serv1.db.schema.{TickerData, TickerDataTable, TickerDataTableGen, TickerDataTableNameUtil}
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

object TickerDataRepo extends TickerDataRepoIntf {
  private val createdTables: concurrent.Map[TickerLoadType, Boolean] =
    new ConcurrentHashMap[TickerLoadType, Boolean]().asScala

  def init(): Unit = {
    val tables = Await.result(DB.db.run(MTable.getTables), Duration.Inf)
    tables.foreach {
      table =>
        TickerDataTableNameUtil.parseTableName(table.name.name).map {
          tt =>
            createdTables.addOne((tt, true))
            TickerTypeRepo.addTickerType(tt)
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
      TickerTypeRepo.addTickerType(ticker)
    }
  }

  def write(ticker: TickerLoadType, data: Seq[HistoricalData]): Unit = {
    createTableIfNotExists(ticker)
    val tableQuery = TickerDataTable.getQuery(ticker)
    val listToInsert = data.map({ hd => TickerData(0, hd.timestamp, hd.open, hd.high, hd.low, hd.close, hd.vol) })
    Await.result(DB.db.run(DBIO.seq(tableQuery ++= listToInsert)), Duration.Inf)
  }

  def read(ticker: TickerLoadType): Seq[HistoricalData] = {
    createTableIfNotExists(ticker)
    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQuery = TableQuery[tickerDataTable.TickerDataTable]
    val tickerData = Await.result(DB.db.run(tableQuery.result), Duration.Inf)
    val tickerDataHD: Seq[HistoricalData] = tickerData.map(
      TickerData.tickerDataToHistoricalData)
    tickerDataHD
  }

  def readRange(ticker: TickerLoadType, from: Long, to: Long): Seq[HistoricalData] = {
    createTableIfNotExists(ticker)
    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQuery = TableQuery[tickerDataTable.TickerDataTable].filter(td => td.time >= from && td.time <= to)
    val tickerData = Await.result(DB.db.run(tableQuery.result), Duration.Inf)
    val tickerDataHD: Seq[HistoricalData] = tickerData.map(
      TickerData.tickerDataToHistoricalData
    )
    tickerDataHD
  }

  def truncate(ticker: TickerLoadType): Unit = {
    createTableIfNotExists(ticker)
    val tableQuery = TickerDataTable.getQuery(ticker)
    Await.result(DB.db.run(DBIO.seq(tableQuery.delete)), Duration.Inf)
  }
}

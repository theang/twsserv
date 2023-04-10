package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.DB.db
import serv1.db.exception.DatabaseException
import serv1.db.repo.intf.TickerDataRepoIntf
import serv1.db.schema.{TickerData, TickerDataTable, TickerDataTableGen, TickerDataTableNameUtil}
import serv1.model.HistoricalData
import serv1.model.ticker.{BarSizes, TickerLoadType}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import slick.util.Logging

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success}

object TickerDataRepo extends TickerDataRepoIntf with Logging with BaseRepo {
  var timeout: FiniteDuration = 5.second
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
    if (ticker.barSize == BarSizes.TICK) {
      throw new DatabaseException("You cant create tick table, create TickLast and TickBidAsk instead, using TickerTick repo")
    }
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
    if (ticker.barSize == BarSizes.TICK) {
      throw new DatabaseException("You cant write tick table using TickerDataRepo, use TickerTick repo")
    }
    createTableIfNotExists(ticker)

    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQueryRead = TableQuery[tickerDataTable.TickerDataTable].filter(td => td.time inSet data.map(_.timestamp)).map(_.time)
    val timesAlreadyInTable: Set[Long] = Await.result(DB.db.run(tableQueryRead.result), timeout).toSet
    val tableQuery = TickerDataTable.getQuery(ticker)
    val listToInsert = data.filter { hd =>
      !timesAlreadyInTable.contains(hd.timestamp)
    }.map({ hd => TickerData(0, hd.timestamp, hd.open, hd.high, hd.low, hd.close, hd.vol) })
    val action = tableQuery ++= listToInsert
    writeWithCheck(listToInsert.size, action, timeout)
  }

  def writeUpdate(ticker: TickerLoadType, data: Seq[HistoricalData]): Unit = {
    if (ticker.barSize == BarSizes.TICK) {
      throw new DatabaseException("You cant write tick table using TickerDataRepo, use TickerTick repo")
    }
    createTableIfNotExists(ticker)

    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQueryDelete = TableQuery[tickerDataTable.TickerDataTable].filter(td => td.time inSet data.map(_.timestamp))
    Await.result(DB.db.run(tableQueryDelete.delete.asTry), timeout) match {
      case Success(res) =>
        logger.debug(s"Delete result: $res")
      case Failure(ex) =>
        logger.error("Delete failed", ex)
        throw new DatabaseException(cause = ex)
    }

    val tableQuery = TickerDataTable.getQuery(ticker)
    val listToInsert = data.map({ hd => TickerData(0, hd.timestamp, hd.open, hd.high, hd.low, hd.close, hd.vol) })
    val action = tableQuery ++= listToInsert
    writeWithCheck(listToInsert.size, action, timeout)
  }

  def read(ticker: TickerLoadType): Seq[HistoricalData] = {
    if (ticker.barSize == BarSizes.TICK) {
      throw new DatabaseException("You cant read tick table using TickerDataRepo, use TickerTick repo")
    }
    createTableIfNotExists(ticker)
    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQuery = TableQuery[tickerDataTable.TickerDataTable]
    val tickerData = Await.result(DB.db.run(tableQuery.result), Duration.Inf)
    val tickerDataHD: Seq[HistoricalData] = tickerData.map(
      TickerData.tickerDataToHistoricalData)
    tickerDataHD
  }

  def readRange(ticker: TickerLoadType, from: Long, to: Long): Seq[HistoricalData] = {
    if (ticker.barSize == BarSizes.TICK) {
      throw new DatabaseException("You cant read tick table using TickerDataRepo, use TickerTick repo")
    }
    createTableIfNotExists(ticker)
    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQuery = TableQuery[tickerDataTable.TickerDataTable].filter(td => td.time >= from && td.time <= to).sortBy(_.time)
    val tickerData = Await.result(DB.db.run(tableQuery.result), Duration.Inf)
    val tickerDataHD: Seq[HistoricalData] = tickerData.map(
      TickerData.tickerDataToHistoricalData
    )
    tickerDataHD
  }

  def latestDate(ticker: TickerLoadType): Option[Long] = {
    createTableIfNotExists(ticker)
    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQuery = TableQuery[tickerDataTable.TickerDataTable].map(_.time).max
    Await.result(DB.db.run(tableQuery.result), Duration.Inf)
  }

  def earliestDate(ticker: TickerLoadType): Option[Long] = {
    if (ticker.barSize == BarSizes.TICK) {
      throw new DatabaseException("You cant read tick table using TickerDataRepo, use TickerTick repo")
    }
    createTableIfNotExists(ticker)
    val tickerDataTable = new TickerDataTableGen(ticker)
    val tableQuery = TableQuery[tickerDataTable.TickerDataTable].map(_.time).min
    Await.result(DB.db.run(tableQuery.result), Duration.Inf)
  }

  def truncate(ticker: TickerLoadType): Unit = {
    if (ticker.barSize == BarSizes.TICK) {
      throw new DatabaseException("You cant truncate tick table using TickerDataRepo, use TickerTick repo")
    }
    createTableIfNotExists(ticker)
    val tableQuery = TickerDataTable.getQuery(ticker)
    Await.result(DB.db.run(DBIO.seq(tableQuery.delete)), Duration.Inf)
  }
}

package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.DB.db
import serv1.db.repo.intf.TickerTickRepoIntf
import serv1.db.schema._
import serv1.model.ticker.TickerLoadType
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import slick.util.Logging

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}
import scala.jdk.CollectionConverters._

object TickerTickRepo extends Logging with BaseRepo with TickerTickRepoIntf {
  var duration: Duration = Duration.Inf
  var writeDuration: Duration = 5.second
  var readDuration: Duration = 5.second

  type TickerTickLastQuery = TableQuery[_ <: TickerTickTimedTable[_ <: TickerTickTimed]]
  type TickerTickBidAskQuery = TableQuery[_ <: TickerTickTimedTable[_ <: TickerTickTimed]]
  type TickerTickQueries = (TickerTickLastQuery, TickerTickBidAskQuery)
  private val createdTables: concurrent.Map[TickerLoadType, TickerTickQueries] =
    new ConcurrentHashMap[TickerLoadType, TickerTickQueries]().asScala

  def createTableIfNotExists(ticker: TickerLoadType): Option[TickerTickQueries] = {
    if (!createdTables.contains(ticker)) {
      val dbTables = Await.result(db.run(MTable.getTables), Duration.Inf).map(_.name.name)
      val tableLastName = TickerDataTableNameUtil.formatTickTableName(ticker, TickerTickTable.tickLast)
      val tableBidAskName = TickerDataTableNameUtil.formatTickTableName(ticker, TickerTickTable.tickBidAsk)
      val tickerTickLastQuery = TickerTickTable.getTickLastQuery(ticker)
      val tickerTickBidAskQuery = TickerTickTable.getTickBidAskQuery(ticker)
      val tickerTickQueries = (tickerTickLastQuery, tickerTickBidAskQuery)
      if (!dbTables.contains(tableLastName)) {
        Await.result(DB.db.run(tickerTickLastQuery.schema.createIfNotExists), duration)
      }
      if (!dbTables.contains(tableBidAskName)) {
        Await.result(DB.db.run(tickerTickBidAskQuery.schema.createIfNotExists), duration)
      }
      createdTables.put(ticker, tickerTickQueries)
    }
    createdTables.get(ticker)
  }


  def writeLast(ticker: TickerLoadType, data: Seq[TickerTickLast]): Unit = {
    createTableIfNotExists(ticker) match {
      case Some((tickerTickLastQuery, _)) =>
        var listToInsert = data
        if (data.nonEmpty) {
          val minTime = data.map(_.time).min
          val maxTime = data.map(_.time).max
          val timesAlreadyInDb = Await.result(DB.db.run(tickerTickLastQuery.filter { td => td.time >= minTime && td.time <= maxTime }.map(_.time).result), readDuration).toSet
          listToInsert = data.filter { ttl => !timesAlreadyInDb.contains(ttl.time) }
        }
        val action = tickerTickLastQuery.asInstanceOf[TableQuery[_ <: Table[TickerTickLast]]] ++= listToInsert
        writeWithCheck(listToInsert.size, action, writeDuration)
      case _ =>
        logger.warn(s"Could not write Last for $ticker, table was not created")
    }
  }

  def writeBidAsk(ticker: TickerLoadType, data: Seq[TickerTickBidAsk]): Unit = {
    createTableIfNotExists(ticker) match {
      case Some((_, tickerTickBidAskQuery)) =>
        var listToInsert = data
        if (data.nonEmpty) {
          val minTime = data.map(_.time).min
          val maxTime = data.map(_.time).max
          val timesAlreadyInDb = Await.result(DB.db.run(tickerTickBidAskQuery.filter { td => td.time >= minTime && td.time <= maxTime }.map(_.time).result), readDuration).toSet
          listToInsert = data.filter { ttl => !timesAlreadyInDb.contains(ttl.time) }
        }
        val action = tickerTickBidAskQuery.asInstanceOf[TableQuery[_ <: Table[TickerTickBidAsk]]] ++= listToInsert
        writeWithCheck(listToInsert.size, action, writeDuration)
      case _ =>
        logger.warn(s"Could not write BidAsk for $ticker, table was not created")
    }
  }

  def readLast(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Seq[TickerTickLast] = {
    createTableIfNotExists(ticker) match {
      case Some(_) =>
        val table = new TickerTickLastTableGen(ticker)
        val tickerTickLastQuery = TableQuery[table.TickerTickLastTable]
        Await.result(DB.db.run(tickerTickLastQuery.filter { ttl => ttl.time >= timeFrom && ttl.time <= timeTo }.result), readDuration)
      case _ =>
        logger.warn(s"Could not read Last for $ticker, table was not created")
        Seq.empty
    }
  }

  def readBidAsk(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Seq[TickerTickBidAsk] = {
    createTableIfNotExists(ticker) match {
      case Some(_) =>
        val table = new TickerTickBidAskTableGen(ticker)
        val tickerTickLastQuery = TableQuery[table.TickerTickBidAskTable]
        Await.result(DB.db.run(tickerTickLastQuery.filter { ttl => ttl.time >= timeFrom && ttl.time <= timeTo }.result), readDuration)
      case _ =>
        logger.warn(s"Could not read BidAsk for $ticker, table was not created")
        Seq.empty
    }
  }

  def removeLast(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Int = {
    createTableIfNotExists(ticker) match {
      case Some((tickerTickLastQuery, _)) =>
        Await.result(DB.db.run(tickerTickLastQuery.filter { td => td.time >= timeFrom && td.time <= timeTo }.delete), readDuration)
      case _ =>
        logger.warn(s"Could not delete Last for $ticker, table was not created")
        0
    }
  }

  def removeBidAsk(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Int = {
    createTableIfNotExists(ticker) match {
      case Some((_, tickerTickBidAskQuery)) =>
        Await.result(DB.db.run(tickerTickBidAskQuery.filter { td => td.time >= timeFrom && td.time <= timeTo }.delete), readDuration)
      case _ =>
        logger.warn(s"Could not delete Last for $ticker, table was not created")
        0
    }
  }
}

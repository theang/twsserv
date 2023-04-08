package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.repo.intf.TickerTypeRepoIntf
import serv1.db.schema.{TickerTypeDB, TickerTypeTable}
import serv1.model.ticker.TickerLoadType
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object TickerTypeRepo extends TickerTypeRepoIntf {
  def addTickerType(tt: TickerLoadType): Unit = {
    val tableQuery = TickerTypeTable.query
    val action = tableQuery.filter(TickerTypeTable.findTicker(tt)).exists.result.flatMap { exists =>
      if (!exists) {
        tableQuery += tt
      } else {
        DBIO.successful(None)
      }
    }.transactionally
    Await.result(DB.db.run(action), Duration.Inf)
  }

  def queryTickers(): Seq[TickerLoadType] = {
    Await.result(DB.db.run(TickerTypeTable.query.result), Duration.Inf).map(
      TickerTypeDB.tickerTypeDBToTickerLoadType)
  }

  def queryTickers(ids: Seq[Int]): Seq[TickerLoadType] = {
    val idsSet = ids.toSet
    Await.result(DB.db.run(TickerTypeTable.query
      .filter(_.id.inSet(idsSet)).result), Duration.Inf).map(
      TickerTypeDB.tickerTypeDBToTickerLoadType)
  }

  def queryTickerType(tt: TickerLoadType): Option[Int] = {
    val tableQuery = TickerTypeTable.query
    val tickerTypes: Seq[Int] = Await.result(DB.db.run(tableQuery.filter(TickerTypeTable.findTicker(tt)).map(_.id).result), Duration.Inf)
    if (tickerTypes.isEmpty) {
      Option.empty
    } else {
      Option(tickerTypes.head)
    }
  }

  def removeTickerType(tt: TickerLoadType): Int = {
    val tableQuery = TickerTypeTable.query
    Await.result(DB.db.run(tableQuery.filter(TickerTypeTable.findTicker(tt)).delete), Duration.Inf)
  }

  def truncate(): Unit = {
    val tableQuery = TickerTypeTable.query
    Await.result(DB.db.run(tableQuery.delete), Duration.Inf)
  }
}

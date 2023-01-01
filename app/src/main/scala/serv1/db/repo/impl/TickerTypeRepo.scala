package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.schema.{TickerTypeDB, TickerTypeTable}
import serv1.model.ticker.TickerLoadType
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object TickerTypeRepo {
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

  def removeTickerType(tt: TickerLoadType): Int = {
    val tableQuery = TickerTypeTable.query
    Await.result(DB.db.run(tableQuery.filter(TickerTypeTable.findTicker(tt)).delete), Duration.Inf)
  }

  def truncate(): Unit = {
    val tableQuery = TickerTypeTable.query
    Await.result(DB.db.run(tableQuery.delete), Duration.Inf)
  }
}

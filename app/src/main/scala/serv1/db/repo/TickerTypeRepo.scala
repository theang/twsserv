package serv1.db.repo

import serv1.db.DB
import serv1.db.schema.TickerTypeTable
import serv1.model.ticker.TickerLoadType
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

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
}

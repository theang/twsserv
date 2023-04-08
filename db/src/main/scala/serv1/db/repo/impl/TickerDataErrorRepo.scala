package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.schema.{TickerDataErrors, TickerDataErrorsTable}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TickerDataErrorRepo {
  def addError(msg: String): Int = {
    val tableQuery = TickerDataErrorsTable.query
    val action = (tableQuery returning tableQuery.map(_.id)) += TickerDataErrors(0, msg)
    Await.result(DB.db.run(action), Duration.Inf)
  }

  def queryMessages: Seq[TickerDataErrors] = {
    Await.result(DB.db.run(TickerDataErrorsTable.query.result), Duration.Inf)
  }

  def queryMessage(id: Int): TickerDataErrors = {
    Await.result(DB.db.run(TickerDataErrorsTable.query.filter(_.id === id).result), Duration.Inf).head
  }

  def deleteMessage(id: Int): Int = {
    Await.result(DB.db.run(TickerDataErrorsTable.query.filter(_.id === id).delete), Duration.Inf)
  }

  def truncate: Int = {
    Await.result(DB.db.run(TickerDataErrorsTable.query.delete), Duration.Inf)
  }
}

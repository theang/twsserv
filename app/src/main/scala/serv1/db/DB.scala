package serv1.db

import serv1.db.schema._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object DB {
  val DBConfigPath = "postgres"
  val db = Database.forConfig(DBConfigPath)

  def createTables(): Seq[Unit] = {
    val dbTables = Await.result(db.run(MTable.getTables), Duration.Inf).map(_.name.name)
    val tables = List(JobTable.query,
      TickerTypeTable.query,
      ScheduledTaskTable.query,
      TickerDataErrorsTable.query,
      TickerTrackingTable.query
    ).filter(q => !dbTables.contains(q.baseTableRow.tableName))
    val action = for (t <- tables) yield t.schema.createIfNotExists
    Await.result(db.run(DBIO.sequence(action)), Duration.Inf)
  }
}

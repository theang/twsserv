package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object ScheduledTaskTable {
  val query = TableQuery[ScheduledTaskTable]
}

class ScheduledTaskTable(tag: Tag) extends Table[ScheduledTask](tag, "SCHEDULED_TASK_TABLE") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

  def name = column[String]("NAME", O.Unique)

  def nextRun = column[Long]("NEXT_RUN")

  def schedule = column[String]("SCHEDULE")

  def * = (id, name, nextRun, schedule) <> (ScheduledTask.tupled, ScheduledTask.unapply)
}
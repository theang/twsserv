package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.schema.{ScheduledTask, ScheduledTaskTable}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ScheduledTaskRepo {
  def addScheduledTask(name: String, schedule: String, nextRun: Long): Int = {
    val tableQuery = ScheduledTaskTable.query
    val scheduledTaskId = (tableQuery returning tableQuery.map(_.id)) += ScheduledTask(0, name, nextRun, schedule)
    Await.result(DB.db.run(scheduledTaskId), Duration.Inf)
  }

  def deleteScheduledTask(name: String): Int = {
    val tableQuery = ScheduledTaskTable.query
    val removeTaskQuery = tableQuery.filter(_.name === name).delete
    Await.result(DB.db.run(removeTaskQuery), Duration.Inf)
  }

  def updateNextRun(name: String, newNextRun: Long): Int = {
    val tableQuery = ScheduledTaskTable.query
    val updateTaskQuery = tableQuery.filter(_.name === name).map(_.nextRun).update(newNextRun)
    Await.result(DB.db.run(updateTaskQuery), Duration.Inf)
  }

  def updateSchedule(name: String, schedule: String): Int = {
    val tableQuery = ScheduledTaskTable.query
    val updateTaskQuery = tableQuery.filter(_.name === name).map(_.schedule).update(schedule)
    Await.result(DB.db.run(updateTaskQuery), Duration.Inf)
  }

  def updateName(name: String, newName: String): Int = {
    val tableQuery = ScheduledTaskTable.query
    val updateTaskQuery = tableQuery.filter(_.name === name).map(_.name).update(newName)
    Await.result(DB.db.run(updateTaskQuery), Duration.Inf)
  }

  def getScheduledTaskById(id: Int): Seq[ScheduledTask] = {
    Await.result(DB.db.run(ScheduledTaskTable.query.filter(_.id === id).result), Duration.Inf)
  }

  def getScheduledTaskByName(name: String): Seq[ScheduledTask] = {
    Await.result(DB.db.run(ScheduledTaskTable.query.filter(_.name === name).result), Duration.Inf)
  }

  def getScheduledTasksBeforeNextRun(nextRun: Long): Seq[ScheduledTask] = {
    Await.result(DB.db.run(ScheduledTaskTable.query.filter(_.nextRun <= nextRun).result), Duration.Inf)
  }

  def truncate: Int = {
    Await.result(DB.db.run(ScheduledTaskTable.query.delete), Duration.Inf)
  }
}

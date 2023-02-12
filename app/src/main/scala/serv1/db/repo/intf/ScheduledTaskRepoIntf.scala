package serv1.db.repo.intf

import serv1.db.schema.ScheduledTask

trait ScheduledTaskRepoIntf {
  def addScheduledTask(name: String, schedule: String, nextRun: Long): Int

  def deleteScheduledTask(name: String): Int

  def updateNextRun(name: String, newNextRun: Long): Int

  def updateSchedule(name: String, schedule: String): Int

  def updateName(name: String, newName: String): Int

  def getScheduledTaskById(id: Int): Seq[ScheduledTask]

  def getScheduledTaskByName(name: String): Seq[ScheduledTask]
}

package serv1.db.schema

case class ScheduledTask(id: Int, name: String, nextRun: Long, schedule: String)

package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import java.util.UUID

object JobTable {
  val query = TableQuery[JobTable]
}

class JobTable(tag: Tag) extends Table[Job](tag, "JOB") {
  def jobId = column[UUID]("JOB_ID", O.PrimaryKey)
  def state = column[String]("STATE")
  def * = (jobId, state) <> (Job.tupled, Job.unapply)
}

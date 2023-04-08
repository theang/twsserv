package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import java.util.UUID

object JobTable {
  val jobTableGen = new JobTableGen(false)
  val query = TableQuery[jobTableGen.JobTable]
  val jobTableGenArchive = new JobTableGen(true)
  val archiveQuery = TableQuery[jobTableGenArchive.JobTable]
}

class JobTableGen(archive: Boolean) {

  val jobTableName = s"JOB${if (archive) "_ARCHIVE" else ""}"

  class JobTable(tag: Tag) extends Table[Job](tag, jobTableName) {
    def jobId = if (archive) column[UUID]("JOB_ID") else column[UUID]("JOB_ID", O.PrimaryKey)

    def state = column[String]("STATE")

    def * = (jobId, state) <> (Job.tupled, Job.unapply)
  }
}

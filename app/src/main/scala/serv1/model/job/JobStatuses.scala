package serv1.model.job

object JobStatuses extends Enumeration {
  type JobStatus = Value

  val IN_PROGRESS, FINISHED, ERROR = Value
}

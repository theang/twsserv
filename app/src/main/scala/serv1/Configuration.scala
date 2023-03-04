package serv1

import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, _}

object Configuration {
  implicit val timeout: Timeout = Timeout(1000, TimeUnit.SECONDS)
  val callDuration: FiniteDuration = 5.second
  val defaultPrecision: Int = 2
  val DATABASE_BLOCKING_DISPATCHER_NAME = "database-blocking-dispatcher"
  val DATABASE_SCHEMA_VERSION_PARAMETER_TYP = "database"
  val DATABASE_SCHEMA_VERSION_PARAMETER_NAME = "DATABASE_SCHEMA_VERSION"
  val DATABASE_SCHEMA_VERSION: String = "1"
  val DATABASE_SCHEMA_UPGRADE_FILE: String = "schema_upgrade.yaml"

  val INITIAL_TICKER_TRACKER_SCHEDULED_TASKS_ENABLED = true
  val INITIAL_RESTART_DATA_LOAD_TASKS_ENABLED = true
  val INITIAL_JOB_REPO_WAIT_LOCK_DURATION = 2
}
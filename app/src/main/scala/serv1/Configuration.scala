package serv1

import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, _}

object Configuration {
  implicit val timeout: Timeout = Timeout(1000, TimeUnit.SECONDS)
  val callDuration: FiniteDuration = 5.second
  val DATABASE_BLOCKING_DISPATCHER_NAME = "database-blocking-dispatcher"

  val INITIAL_TICKER_TRACKER_SCHEDULED_TASKS_ENABLED = true
  val INITIAL_RESTART_DATA_LOAD_TASKS_ENABLED = true
}
package serv1

import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, _}

object Configuration {
  implicit val timeout: Timeout = Timeout(1000, TimeUnit.SECONDS)
  val callDuration: FiniteDuration = 5.second
  val defaultPrecision: Int = 2
  val DATABASE_BLOCKING_DISPATCHER_NAME = "database-blocking-dispatcher"
}
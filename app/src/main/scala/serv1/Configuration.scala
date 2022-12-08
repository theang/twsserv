package serv1

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object Configuration {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  val callDuration: FiniteDuration = 5.second
  val defaultPrecision: Int = 2
}
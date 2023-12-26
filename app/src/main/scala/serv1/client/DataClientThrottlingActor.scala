package serv1.client

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import serv1.config.ServConfig

object DataClientThrottlingActor {
  var config: Config = ServConfig.config.getConfig("throttlingActor")
  var identicalHistoricalRequestsCoolDownSeconds = config.getInt("identicalHistoricalRequestsCoolDownSeconds")
  var simultaneousHistoricalRequests = config.getInt("simultaneousHistoricalRequests")
  var sizeLimit = config.getInt("sizeLimit")

  sealed trait Message

  case class LoadHistoricalData() extends Message

  case object TimerKey

  def apply(): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        Behaviors.receive[Message] {
          case (_, _) =>
            Behaviors.same
        }
    }
  }
}

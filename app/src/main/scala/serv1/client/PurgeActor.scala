package serv1.client

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import serv1.client.operations.ClientOperationHandlers
import serv1.config.ServConfig

import scala.concurrent.duration.{FiniteDuration, SECONDS}

object PurgeActor {
  val config: Config = ServConfig.config.getConfig("purgeActor")
  var intervalValue: Int = config.getInt("interval")
  var interval: FiniteDuration = FiniteDuration.apply(intervalValue, SECONDS)

  sealed trait Message

  case object PurgeMessage extends Message

  case object TimerKey

  def apply(): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        timers.startTimerWithFixedDelay(TimerKey, PurgeMessage, interval, interval)
        Behaviors.setup[Message] {
          context =>
            Behaviors.receiveMessage {
              case PurgeMessage =>
                ClientOperationHandlers.purgeAllData()
                Behaviors.same
            }
        }
    }
  }
}

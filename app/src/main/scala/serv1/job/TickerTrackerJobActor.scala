package serv1.job

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import serv1.job.TickerJobActor.Run

object TickerTrackerJobActor {

  sealed trait Message

  object GetStatus extends Message

  sealed class CheckTrackedTickets extends Message

  sealed trait ResultMessage

  def apply(): Behavior[Message] = {
    Behaviors.withTimers {
        timers =>
          Behaviors.setup {
            context =>

            Behaviors.receiveMessage {
              case GetStatus =>
                Behaviors.same
            }
        }
    }
  }
}

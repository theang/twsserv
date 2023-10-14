package serv1.schedule

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import serv1.util.LocalDateTimeUtil

import scala.concurrent.duration._

object ScheduledJobActor {
  val DELAY: FiniteDuration = 30.seconds
  val CALL_EVERY: FiniteDuration = 60.seconds

  sealed trait Message

  object GetStatus extends Message

  object CheckTrackedTickers extends Message

  sealed trait ResultMessage

  case object TimerKey

  def apply(scheduledJobService: ScheduledJobService): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        timers.startTimerWithFixedDelay(TimerKey, CheckTrackedTickers, DELAY, CALL_EVERY)
        Behaviors.setup[Message] {
          context =>
            Behaviors.receiveMessage {
              case GetStatus =>
                Behaviors.same
              case CheckTrackedTickers =>
                scheduledJobService.runCurrentScheduledJobs(LocalDateTimeUtil.toEpoch(LocalDateTimeUtil.now()))
                Behaviors.same
            }
        }
    }
  }
}

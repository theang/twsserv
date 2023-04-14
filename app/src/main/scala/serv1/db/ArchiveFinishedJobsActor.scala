package serv1.db

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import serv1.db.repo.intf.JobRepoIntf
import slick.util.Logging

import scala.concurrent.duration.{FiniteDuration, _}

object ArchiveFinishedJobsActor extends Logging {
  val DELAY: FiniteDuration = 30.seconds
  val CALL_EVERY: FiniteDuration = 60.seconds

  sealed trait Message

  case object ArchiveFinishedJobs extends Message

  case object TimerKey

  def apply(jobRepo: JobRepoIntf): Behavior[Message] = {
    Behaviors.withTimers { timers =>
      timers.startTimerWithFixedDelay(TimerKey, ArchiveFinishedJobs, DELAY, CALL_EVERY)
      Behaviors.receive[Message] {
        case (context, ArchiveFinishedJobs) =>
          jobRepo.archiveCompletedJobs()
          Behaviors.same
      }
    }
  }
}

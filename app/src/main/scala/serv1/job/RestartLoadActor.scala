package serv1.job

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import serv1.Configuration.INITIAL_RESTART_DATA_LOAD_TASKS_ENABLED
import serv1.db.repo.intf.JobRepoIntf
import serv1.job.TickerJobActor.{Run, RunningState}
import serv1.model.job.JobStatuses
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

object RestartLoadActor extends Logging {
  val DELAY: FiniteDuration = 10.seconds
  val CALL_EVERY: FiniteDuration = 60.seconds

  var restart: Boolean = INITIAL_RESTART_DATA_LOAD_TASKS_ENABLED

  implicit val timeout: Timeout = 1000.seconds

  sealed trait Message

  case object Restart extends Message

  case object TimerKey

  def apply(jobRepoIntf: JobRepoIntf, tickerJobActor: ActorRef[TickerJobActor.JobActorMessage])(implicit system: ActorSystem[_]): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        timers.startTimerWithFixedDelay(TimerKey, Restart, DELAY, CALL_EVERY)
        Behaviors.setup[Message] {
          context =>
            Behaviors.receiveMessage {
              case Restart =>
                val ids = jobRepoIntf.getTickerJobsByStates(Set(JobStatuses.IN_PROGRESS, JobStatuses.ERROR)).map {
                  case (id, _) =>
                    id
                }
                val runningState: RunningState = Await.result(tickerJobActor.ask(replyTo => TickerJobActor.CheckState(ids.toSet, replyTo)), timeout.duration).asInstanceOf[RunningState]
                val notRunningIds = runningState.jobIdsRunning.filter { case (_, isRunning) => !isRunning }.map { case (id, _) => id }.toSeq
                logger.info(s"Restarting jobs: $notRunningIds")
                if (restart) {
                  notRunningIds.map { id => tickerJobActor.ask(replyTo => Run(id, replyTo)) }
                } else {
                  logger.info("serv1.job.RestartLoadActor.restart option is disabled")
                }
                Behaviors.same
            }
        }
    }

  }
}

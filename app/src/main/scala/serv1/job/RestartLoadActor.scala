package serv1.job

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import serv1.Configuration.INITIAL_RESTART_DATA_LOAD_TASKS_ENABLED
import serv1.db.repo.intf.JobRepoIntf
import serv1.job.EarningsJobActor.StartEarningsLoading
import serv1.job.TickerJobActor.{Run, RunningState}
import serv1.model.job._
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration._

object RestartLoadActor extends Logging {
  val DELAY: FiniteDuration = 10.seconds
  val CALL_EVERY: FiniteDuration = 60.seconds

  var restart: Boolean = INITIAL_RESTART_DATA_LOAD_TASKS_ENABLED

  implicit val timeout: Timeout = 1000.seconds

  sealed trait Message

  case object Restart extends Message

  case object TimerKey

  object JobKinds extends Enumeration {
    type JobKind = Value

    val TICKER_JOB, EARNINGS_JOB = Value
  }

  def jobKind(jobState: JobState): JobKinds.JobKind = {
    jobState match {
      case t: TickerJobState => JobKinds.TICKER_JOB
      case t: TickLoadingJobState => JobKinds.TICKER_JOB
      case t: EarningsLoadingJobState => JobKinds.EARNINGS_JOB
    }
  }

  def restart(jobRepoIntf: JobRepoIntf, tickerJobActor: ActorRef[TickerJobActor.JobActorMessage], earningsJobActor: ActorRef[EarningsJobActor.EarningsJobActorMessage])(implicit system: ActorSystem[_]): Unit = {
    val notFinishedJobs = jobRepoIntf.getTickerJobsByStates[JobState](Set(JobStatuses.IN_PROGRESS, JobStatuses.ERROR))
    val notFinishedJobByKind = notFinishedJobs.groupMap { case (_, state) => jobKind(state) } { case (id, _) => id }
    val ids = notFinishedJobByKind.getOrElse(JobKinds.TICKER_JOB, Seq.empty)
    val runningState: RunningState = Await.result(tickerJobActor.ask(replyTo => TickerJobActor.CheckState(ids.toSet, replyTo)), timeout.duration).asInstanceOf[RunningState]
    val notRunningIds = runningState.jobIdsRunning.filter { case (_, isRunning) => !isRunning }.map { case (id, _) => id }.toSeq

    val earningsIds = notFinishedJobByKind.getOrElse(JobKinds.EARNINGS_JOB, Seq.empty)

    logger.info(s"Restarting jobs: $notRunningIds, earningsIds: $earningsIds")
    if (restart) {
      notRunningIds.map { id => tickerJobActor.ask(replyTo => Run(id, replyTo)) }
      earningsIds.map { id => earningsJobActor.ask(replyTo => StartEarningsLoading(id, replyTo)) }
    } else {
      logger.info("serv1.job.RestartLoadActor.restart option is disabled")
    }
  }

  def apply(jobRepoIntf: JobRepoIntf, tickerJobActor: ActorRef[TickerJobActor.JobActorMessage], earningsJobActor: ActorRef[EarningsJobActor.EarningsJobActorMessage])(implicit system: ActorSystem[_]): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        timers.startTimerWithFixedDelay(TimerKey, Restart, DELAY, CALL_EVERY)
        Behaviors.setup[Message] {
          context =>
            Behaviors.receiveMessage {
              case Restart =>
                restart(jobRepoIntf, tickerJobActor, earningsJobActor)
                Behaviors.same
            }
        }
    }

  }
}

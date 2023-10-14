package serv1.job

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import serv1.client.NasdaqClient
import serv1.db.repo.intf.JobRepoIntf
import serv1.model.job.{EarningsLoadingJobState, JobStatuses}
import slick.util.Logging

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object EarningsJobActor extends Logging {
  sealed trait EarningsJobActorMessage

  case class StartEarningsLoading(earningsJobId: UUID, replyTo: ActorRef[ResponseMessage]) extends EarningsJobActorMessage

  case class StopEarningsLoading(earningsJobId: UUID, replyTo: ActorRef[ResponseMessage]) extends EarningsJobActorMessage

  case class RestartEarningsLoading(earningsJobId: UUID, replyTo: ActorRef[ResponseMessage]) extends EarningsJobActorMessage

  case class LoadStep(earningsJobId: UUID, day: LocalDateTime) extends EarningsJobActorMessage

  sealed trait ResponseMessage

  case class RunSuccessful() extends ResponseMessage

  case class RunFailed() extends ResponseMessage

  case class StopSuccessful() extends ResponseMessage

  var currentJobId: Option[UUID] = None

  def nextStep(timers: TimerScheduler[EarningsJobActorMessage], job: UUID, day: LocalDateTime): Unit = {
    timers.startSingleTimer(LoadStep(job, day), FiniteDuration(NasdaqClient.delayTillNextRequest(), MILLISECONDS))
  }

  def withJobState(jobRepo: JobRepoIntf, jobId: UUID, success: EarningsLoadingJobState => Unit, error: () => Unit): Unit = {
    jobRepo.getJobStates(jobId) match {
      case Seq((_, state: EarningsLoadingJobState)) =>
        if (state.status == JobStatuses.IN_PROGRESS) {
          logger.info(s"Running earnings job: $jobId")
          success(state)
        } else {
          logger.error(s"Job: $jobId is not in progress or error")
          error()
        }
      case _ =>
        logger.error(s"Job: $jobId is not an earnings job")
        error()
    }
  }

  def apply(jobRepo: JobRepoIntf, jobService: EarningsJobService): Behavior[EarningsJobActorMessage] = {
    Behaviors.withTimers { timers =>
      Behaviors.receiveMessage {
        case StartEarningsLoading(earningsJobId, replyTo) =>
          if (currentJobId.isEmpty) {
            withJobState(jobRepo, earningsJobId, { state =>
              currentJobId = Some(earningsJobId)
              nextStep(timers, earningsJobId, state.current)
              replyTo ! RunSuccessful()
            }, () => {
              replyTo ! RunFailed()
            })
          } else {
            logger.info(s"Cannot run job: $earningsJobId because current job is ${currentJobId.get}")
            replyTo ! RunFailed()
          }
          Behaviors.same
        case StopEarningsLoading(earningsJobId, replyTo) =>
          currentJobId = None
          jobRepo.finishEarningsLoadingJob(earningsJobId)
          replyTo ! StopSuccessful()
          Behaviors.same
        case LoadStep(earningsJobId, day) =>
          currentJobId match {
            case Some(currentEarningsJobId) =>
              if (currentEarningsJobId == earningsJobId) {
                withJobState(jobRepo, earningsJobId, {
                  state =>
                    jobService.loadEarningsForDay(earningsJobId, day)
                    jobService.updateEarningsJob(earningsJobId, day, state.to) match {
                      case Some(nextDay) => nextStep(timers, earningsJobId, nextDay)
                      case None => logger.info(s"Job: $earningsJobId is finished")
                    }
                }, () => {
                  logger.info(s"Cannot proceed for job: $earningsJobId")
                })
              } else {
                logger.info(s"Cannot proceed for job: $earningsJobId because current job is set to $currentEarningsJobId")
              }
            case None =>
              logger.info(s"Cannot proceed for job: $earningsJobId because there is no current job")
          }
          Behaviors.same
      }
    }
  }
}

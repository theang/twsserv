package serv1.job

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.db.repo.intf.JobRepoIntf
import serv1.model.job.JobStatuses
import serv1.model.ticker.TickerLoadType
import slick.util.Logging

import java.util.UUID

object TickerJobActor extends Logging {

  sealed trait JobActorMessage

  case class Run(jobId: UUID, replyTo: ActorRef[JobActorResponse]) extends JobActorMessage

  case class Finished(jobId: UUID) extends JobActorMessage

  case class CheckState(jobId: Set[UUID], replyTo: ActorRef[JobActorResponse]) extends JobActorMessage


  sealed trait JobActorResponse

  case class RunSuccessful() extends JobActorResponse

  case class RunningState(jobIdsRunning: Map[UUID, Boolean]) extends JobActorResponse

  var runningJobs: Set[UUID] = Set()

  def apply(ticketJobService: TickerJobService, jobRepo: JobRepoIntf): Behavior[JobActorMessage] = {
    Behaviors.receiveMessage[JobActorMessage] {
      case Run(jobId, replyTo) =>
        val state = jobRepo.getTickerJobStates(jobId).head
        logger.info(s"Running job: $jobId")
        if (!runningJobs.contains(jobId) && state.status != JobStatuses.FINISHED) {
          runningJobs += jobId
          val state = jobRepo.getTickerJobStates(jobId).head
          val tickersToProcess: List[TickerLoadType] = (state.tickers diff state.loadedTickers) diff state.ignoredTickers
          ticketJobService.loadTickers(jobId, tickersToProcess, state.from, state.to)
          replyTo ! RunSuccessful()
        } else if (runningJobs.contains(jobId)) {
          logger.warn(s"JobId $jobId is already running by this Actor")
        } else {
          logger.warn(s"JobId $jobId state is Finished, to start it it needs to be in ERROR or IN_PROGRESS state")
        }
        Behaviors.same
      case Finished(jobId) =>
        logger.info(s"Job $jobId is finished")
        val state = jobRepo.getTickerJobStates(jobId).head
        if (!Set(JobStatuses.FINISHED, JobStatuses.ERROR).contains(state.status)) {
          logger.warn(s"Could not finish $jobId because its state is not finished and not error")
        } else {
          runningJobs -= jobId
        }
        Behaviors.same
      case CheckState(jobIds, replyTo) =>
        val result = jobIds.map {
          id => (id, runningJobs.contains(id))
        }.toMap
        replyTo ! RunningState(result)
        Behaviors.same
    }
  }
}

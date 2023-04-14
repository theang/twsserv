package serv1.rest.actors.schedule

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.db.repo.intf.ScheduledTaskRepoIntf
import serv1.job.TickerTrackerJobService
import serv1.util.{CronUtil, LocalDateTimeUtil}

import java.time.LocalDateTime

object ScheduleActor {

  case class RunScheduledTaskRequest(name: Option[String], id: Option[Int], currentDateTime: Option[LocalDateTime])

  case class CreateScheduledTaskRequest(name: String, schedule: String)

  case class RenameTaskRequest(name: String, newName: String)

  case class ChangeScheduleRequest(name: String, newSchedule: String)

  sealed trait RequestMessage

  case class RunScheduledTaskRequestRef(runScheduledTaskRequest: RunScheduledTaskRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class CreateScheduledTaskRequestRef(createScheduledTaskRequest: CreateScheduledTaskRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class RenameTaskRequestRef(renameTaskRequest: RenameTaskRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class ChangeScheduleRequestRef(changeScheduleRequest: ChangeScheduleRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class QueryScheduledTaskRef(id: Int, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class QueryAllScheduledTasksRef(replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  sealed trait ResponseMessage

  case class ScheduledTaskResponse(id: Int, name: String, schedule: String, nextRun: LocalDateTime) extends ResponseMessage

  case class ScheduledTasksResponse(tasks: Seq[ScheduledTaskResponse]) extends ResponseMessage

  def apply(scheduledTaskRepoIntf: ScheduledTaskRepoIntf,
            tickerTrackerJobService: TickerTrackerJobService): Behavior[RequestMessage] = {
    Behaviors.receiveMessage {
      case CreateScheduledTaskRequestRef(createScheduledTaskRequest, replyTo) =>
        val currentDate = LocalDateTimeUtil.getCurrentDateTimeUTC
        val nextRun = CronUtil.findNextRun(LocalDateTimeUtil.toEpoch(currentDate), createScheduledTaskRequest.schedule)
        val taskId = scheduledTaskRepoIntf.addScheduledTask(createScheduledTaskRequest.name, createScheduledTaskRequest.schedule, nextRun)
        val task = scheduledTaskRepoIntf.getScheduledTaskById(taskId).head
        replyTo ! ScheduledTaskResponse(taskId, task.name, task.schedule, LocalDateTimeUtil.fromEpoch(task.nextRun))
        Behaviors.same
      case RenameTaskRequestRef(renameTaskRequest, replyTo) =>
        scheduledTaskRepoIntf.updateName(renameTaskRequest.name, renameTaskRequest.newName)
        val task = scheduledTaskRepoIntf.getScheduledTaskByName(renameTaskRequest.newName).head
        replyTo ! ScheduledTaskResponse(task.id, task.name, task.schedule, LocalDateTimeUtil.fromEpoch(task.nextRun))
        Behaviors.same
      case ChangeScheduleRequestRef(changeScheduleRequest, replyTo) =>
        scheduledTaskRepoIntf.updateSchedule(changeScheduleRequest.name, changeScheduleRequest.newSchedule)
        val task = scheduledTaskRepoIntf.getScheduledTaskByName(changeScheduleRequest.name).head
        replyTo ! ScheduledTaskResponse(task.id, task.name, task.schedule, LocalDateTimeUtil.fromEpoch(task.nextRun))
        Behaviors.same
      case RunScheduledTaskRequestRef(runScheduledTaskRequest, replyTo) =>
        val task = if (runScheduledTaskRequest.id.isEmpty) {
          scheduledTaskRepoIntf.getScheduledTaskByName(runScheduledTaskRequest.name.get).head
        } else {
          scheduledTaskRepoIntf.getScheduledTaskById(runScheduledTaskRequest.id.get).head
        }
        val currentEpoch = LocalDateTimeUtil.toEpoch(runScheduledTaskRequest.currentDateTime.getOrElse(LocalDateTimeUtil.getCurrentDateTimeUTC))
        tickerTrackerJobService.runTrackingJob(currentEpoch, task.name, task.id)
        replyTo ! ScheduledTaskResponse(task.id, task.name, task.schedule, LocalDateTimeUtil.fromEpoch(task.nextRun))
        Behaviors.same
      case QueryAllScheduledTasksRef(replyTo) =>
        replyTo ! ScheduledTasksResponse(scheduledTaskRepoIntf.getAllScheduledTask.map { task =>
          ScheduledTaskResponse(task.id, task.name, task.schedule, LocalDateTimeUtil.fromEpoch(task.nextRun))
        })
        Behaviors.same
      case QueryScheduledTaskRef(id, replyTo) =>
        replyTo ! scheduledTaskRepoIntf.getScheduledTaskById(id).map { task =>
          ScheduledTaskResponse(task.id, task.name, task.schedule, LocalDateTimeUtil.fromEpoch(task.nextRun))
        }.head
        Behaviors.same
    }
  }
}

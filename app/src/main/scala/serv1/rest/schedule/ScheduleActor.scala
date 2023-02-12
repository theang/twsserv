package serv1.rest.schedule

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.db.repo.intf.ScheduledTaskRepoIntf
import serv1.util.{CronUtil, LocalDateTimeUtil}

import java.time.LocalDateTime

object ScheduleActor {

  case class CreateScheduledTaskRequest(name: String, schedule: String)

  case class RenameTaskRequest(name: String, newName: String)

  case class ChangeScheduleRequest(name: String, newSchedule: String)

  sealed trait RequestMessage

  case class CreateScheduledTaskRequestRef(createScheduledTaskRequest: CreateScheduledTaskRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class RenameTaskRequestRef(renameTaskRequest: RenameTaskRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  case class ChangeScheduleRequestRef(changeScheduleRequest: ChangeScheduleRequest, replyTo: ActorRef[ResponseMessage]) extends RequestMessage

  sealed trait ResponseMessage

  case class ScheduledTaskResponse(id: Int, name: String, schedule: String, nextRun: LocalDateTime) extends ResponseMessage

  def apply(scheduledTaskRepoIntf: ScheduledTaskRepoIntf): Behavior[RequestMessage] = {
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
    }
  }
}

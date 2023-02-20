package serv1.rest.schedule

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs._
import serv1.rest.JsonFormats
import serv1.rest.schedule.ScheduleActor._

import scala.concurrent.duration._

@Path("/schedule")
class Schedule(scheduleActor: ActorRef[RequestMessage])(implicit system: ActorSystem[_])
  extends Directives with JsonFormats {
  implicit val timeout: Timeout = 1000.seconds

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Create schedule task", description = "Creates named task",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[CreateScheduledTaskRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "CreateScheduledData response",
        content = Array(new Content(schema = new Schema(implementation = classOf[ScheduledTaskResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def createScheduledTask: Route =
    path("schedule") {
      post {
        entity(as[CreateScheduledTaskRequest]) { request =>
          val result = scheduleActor.ask(replyTo => CreateScheduledTaskRequestRef(
            request, replyTo))
          complete {
            result.mapTo[ScheduledTaskResponse]
          }
        }
      }
    }

  @PATCH
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Rename schedule task", description = "Renames task",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[RenameTaskRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "CreateScheduledData response",
        content = Array(new Content(schema = new Schema(implementation = classOf[ScheduledTaskResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def renameScheduledTask: Route =
    path("schedule/rename") {
      post {
        entity(as[RenameTaskRequest]) { request =>
          val result = scheduleActor.ask(replyTo => RenameTaskRequestRef(
            request, replyTo))
          complete {
            result.mapTo[ScheduledTaskResponse]
          }
        }
      }
    }

  @PATCH
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Change schedule of the scheduled task", description = "Changes schedule task",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[ChangeScheduleRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "CreateScheduledData response",
        content = Array(new Content(schema = new Schema(implementation = classOf[ScheduledTaskResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def changeScheduleOfScheduledTask: Route =
    path("schedule/change") {
      post {
        entity(as[ChangeScheduleRequest]) { request =>
          val result = scheduleActor.ask(replyTo => ChangeScheduleRequestRef(
            request, replyTo))
          complete {
            result.mapTo[ScheduledTaskResponse]
          }
        }
      }
    }

  def routes: Route = {
    createScheduledTask ~ renameScheduledTask ~ changeScheduleOfScheduledTask
  }
}

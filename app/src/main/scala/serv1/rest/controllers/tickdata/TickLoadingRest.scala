package serv1.rest.controllers.tickdata

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs._
import jakarta.ws.rs.core.MediaType
import serv1.rest.JsonFormats
import serv1.rest.actors.loaddata.LoadDataActor._

import scala.concurrent.duration._

@Path("tickLoading")
class TickLoadingRest(loadDataActor: ActorRef[Message])(implicit system: ActorSystem[_])
  extends Directives with JsonFormats {
  implicit var timeout: Timeout = 1000.seconds

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Continuous tick loading", description = "Request continuous tick loading for the ticker, last and bid-ask",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[StartLoadingTickDataRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "StartLoadingTickDataResponse",
        content = Array(new Content(schema = new Schema(implementation = classOf[StartLoadingTickDataResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def startLoadTickData: Route =
    path("tickLoading") {
      post {
        entity(as[StartLoadingTickDataRequest]) { request =>
          val result = loadDataActor.ask(replyTo => StartLoadingTickDataRequestRef(request, replyTo))
          complete {
            result.mapTo[StartLoadingTickDataResponse]
          }
        }
      }
    }

  @DELETE
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Stop continuous tick loading for the ticker", description = "Stop continuous tick loading for the ticker, last and bid-ask",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[StopLoadingTickDataRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Success"),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def stopLoadTickData: Route =
    path("tickLoading") {
      delete {
        entity(as[StopLoadingTickDataRequest]) { request =>
          loadDataActor ! request
          complete(StatusCodes.NoContent, HttpEntity.Empty)
        }
      }
    }

  def routes: Route = {
    startLoadTickData ~ stopLoadTickData
  }
}

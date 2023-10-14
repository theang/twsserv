package serv1.rest.controllers.earnings

import akka.http.scaladsl.server.{Directives, Route}
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.{Consumes, POST, Path, Produces}
import serv1.rest.JsonFormats
import serv1.rest.actors.earnings.EarningsLoadActor.{EarningsLoadRequest, EarningsLoadResponse, StopEarningsLoadRequest, StopEarningsLoadResponse}
import serv1.rest.services.loaddata.earnings.EarningsLoadService

@Path("earningsLoad")
class EarningsLoadRest(earningsLoadService: EarningsLoadService)
  extends Directives with JsonFormats {
  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Load Earnings data", description = "Load earnings data for a period",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[EarningsLoadRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Load Earnings Data response",
        content = Array(new Content(schema = new Schema(implementation = classOf[EarningsLoadResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def loadEarningsData: Route =
    path("loadEarningsData") {
      post {
        entity(as[EarningsLoadRequest]) { request =>
          val result = earningsLoadService.createEarningsJob(request.from, request.to)
          complete(EarningsLoadResponse(result))
        }
      }
    }

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Stop Load Earnings data", description = "Stop Load earnings data",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[StopEarningsLoadRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "stop load data earnings response",
        content = Array(new Content(schema = new Schema(implementation = classOf[StopEarningsLoadResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def stopLoadEarningsData: Route =
    path("stopLoadEarningsData") {
      post {
        entity(as[StopEarningsLoadRequest]) { request =>
          val result = earningsLoadService.stopEarningsJob(request.jobId)
          complete(StopEarningsLoadResponse(result))
        }
      }
    }

  def routes: Route = {
    loadEarningsData ~ stopLoadEarningsData
  }
}


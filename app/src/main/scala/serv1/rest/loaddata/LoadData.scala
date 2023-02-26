package serv1.rest.loaddata

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.{Consumes, POST, Path, Produces}
import serv1.job.TickerJobState
import serv1.rest.JsonFormats
import serv1.rest.historical.HistoricalDataActor.{HistoricalDataRequest, HistoricalDataResponse}
import serv1.rest.loaddata.CheckLoadJobStateActor.CheckLoadJobRef
import serv1.rest.loaddata.LoadDataActor.{LoadDataRequest, LoadDataRequestRef, LoadDataResponse}

import java.util.UUID
import scala.concurrent.duration._

@Path("loadData")
class LoadData(loadDataActor: ActorRef[LoadDataRequestRef],
               checkLoadJobState: ActorRef[CheckLoadJobRef],
               historicalDataActor: ActorRef[HistoricalDataRequest])(implicit system: ActorSystem[_])
  extends Directives with JsonFormats {
  implicit val timeout: Timeout = 1000.seconds

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Load Ticker data from TWS", description = "Load ticker data",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[LoadDataRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "LoadData response",
        content = Array(new Content(schema = new Schema(implementation = classOf[LoadDataResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def loadData: Route =
    path("loadData") {
      post {
        entity(as[LoadDataRequest]) { request =>
          val result = loadDataActor.ask(replyTo => LoadDataRequestRef(request, replyTo))
          complete {
            result.mapTo[LoadDataResponse]
          }
        }
      }
    }

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Check job state", description = "Check jobs state by id",
    requestBody = new RequestBody(required = false,
      content = Array(new Content(schema = new Schema(implementation = classOf[UUID])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Check job state response",
        content = Array(new Content(schema = new Schema(implementation = classOf[List[TickerJobState]])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def checkJobState: Route =
    path("checkJobState") {
      post {
        entity(as[UUID]) { id =>
          val result = checkLoadJobState.ask(replyTo => CheckLoadJobRef(id, replyTo))
          complete {
            result.mapTo[List[TickerJobState]]
          }
        }
      }
    }

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get candle data", description = "Get candle data",
    requestBody = new RequestBody(required = false,
      content = Array(new Content(schema = new Schema(implementation = classOf[LoadDataRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Check job state response",
        content = Array(new Content(schema = new Schema(implementation = classOf[HistoricalDataResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def historicalData: Route =
    path("historical") {
      post {
        entity(as[LoadDataRequest]) { request =>
          val result = historicalDataActor.ask(replyTo => HistoricalDataRequest(request.tickers, request.period, replyTo))
          complete {
            result.mapTo[HistoricalDataResponse]
          }
        }
      }
    }

  def routes: Route = {
    loadData ~ checkJobState ~ historicalData
  }
}

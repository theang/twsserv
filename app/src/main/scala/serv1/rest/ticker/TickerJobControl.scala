package serv1.rest.ticker

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.{Consumes, PATCH, Path, Produces}
import serv1.rest.JsonFormats
import serv1.rest.ticker.TickerJobControlActor._
import slick.util.Logging

import scala.concurrent.duration._

@Path("/tickerJob")
class TickerJobControl(tickerJobControlActor: ActorRef[RequestMessage])(implicit system: ActorSystem[_])
  extends Directives with JsonFormats with Logging {
  implicit val timeout: Timeout = 1000.seconds

  @PATCH
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Add ticker to a tickerJob", description = "Adds ticker to a ticker job",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[AddTickersTrackingRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "TickersTrackingResponse response",
        content = Array(new Content(schema = new Schema(implementation = classOf[TickersTrackingResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def addTickers(): Route =
    path("/tickerJob/addTickers") {
      post {
        entity(as[AddTickersTrackingRequest]) { request =>
          logger.debug(s"getTickets called: $request")
          val result = tickerJobControlActor.ask(replyTo => AddTickersTrackingRequestRef(
            request, replyTo))
          logger.debug(s"getTickets called result: $result")
          complete {
            result.mapTo[TickersTrackingResponse]
          }
        }
      }
    }

  @PATCH
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Remove tickers from a tickerJob", description = "Rmoves tickers from a ticker job",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[RemoveTickersTrackingRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "TickersTrackingResponse response",
        content = Array(new Content(schema = new Schema(implementation = classOf[TickersTrackingResponse])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def removeTickers(): Route =
    path("/tickerJob/removeTickers") {
      post {
        entity(as[RemoveTickersTrackingRequest]) { request =>
          logger.debug(s"removeTickers called: $request")
          val result = tickerJobControlActor.ask(replyTo => RemoveTickersTrackingRequestRef(
            request, replyTo))
          logger.debug(s"removeTickers called result: $result")
          complete {
            result.mapTo[TickersTrackingResponse]
          }
        }
      }
    }

  def routes: Route = {
    addTickers ~ removeTickers
  }
}

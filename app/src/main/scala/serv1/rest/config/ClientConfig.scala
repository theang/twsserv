package serv1.rest.config

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import akka.http.scaladsl.server.{Directives, Route}
import jakarta.ws.rs.{Consumes, GET, POST, Produces}
import serv1.rest.JsonFormats
import jakarta.ws.rs.core.MediaType
import serv1.client.MultiClient
import spray.json.RootJsonFormat

class ClientConfig extends Directives with JsonFormats {
  case class Client(clientName: String)
  implicit val clientFormat: RootJsonFormat[Client] = jsonFormat1(Client)

  @GET
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Currently selected client", description = "Currently selected client",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Current client name",
        content = Array(new Content(schema = new Schema(implementation = classOf[Client])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def loadData: Route =
    path("clientConfig") {
      get {
        complete { Client(MultiClient.currentClient) }
      }
    }
}

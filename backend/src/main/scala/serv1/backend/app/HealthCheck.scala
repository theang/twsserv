package serv1.backend.app

import zio.http._

object HealthCheck {
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collect[Request] {
      case Method.GET -> !! / "healthcheck" =>
        Response.status(Status.NoContent)
    }
}
package serv1.backend.app

import zio._
import zio.http._

trait ErrorHandler {
  def error(exc: Throwable): ZIO[Any, Nothing, Response] = {
    ZIO.debug(s"Exception: $exc").as(Response(body = Body.fromString(s"$exc"), status = Status.InternalServerError))
  }
}

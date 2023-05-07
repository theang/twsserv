package serv1.backend.app

import serv1.backend.db.TickerTypeRepo
import serv1.backend.dto.TickerTypeRequest
import serv1.backend.json.JsonCommon
import serv1.db.schema.TickerTypeDB
import zio._
import zio.http._
import zio.json._

object TickerType extends JsonCommon with ErrorHandler {

  implicit val tickerTypeJson: JsonCodec[TickerTypeDB] = DeriveJsonCodec.gen

  def apply(): Http[TickerTypeRepo, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "tickerType" =>
        TickerTypeRepo.tickerTypes.map { data =>
          Response.json(data.toJson)
        }
      case req@Method.POST -> !! / "tickerType" =>
        for {
          processedReq <- req.body.asString.map(_.fromJson[TickerTypeRequest])
          result <- processedReq match {
            case Left(error) =>
              ZIO.debug(s"Error: $error").as(Response(status = Status.BadRequest, body = Body.fromString(s"$error")))
            case Right(tickerTypeRequest: TickerTypeRequest) =>
              TickerTypeRepo.tickerTypeByRequest(tickerTypeRequest).map {
                data => Response.json(data.toJson)
              }.catchAll(error)
          }
        } yield result
    }
}

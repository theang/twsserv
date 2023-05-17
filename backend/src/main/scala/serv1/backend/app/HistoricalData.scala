package serv1.backend.app

import serv1.backend.db.HistoricalDataRepo
import serv1.backend.dto.{HistoricalDataRequest, HistoricalDataRequestDT}
import serv1.backend.serialize.SerializeTOHLCV
import serv1.db.schema.TickerData
import serv1.util.LocalDateTimeUtil
import zio._
import zio.http._
import zio.json._
import zio.stream.ZStream


object HistoricalData extends ErrorHandler {

  implicit val tickerDataJson: JsonCodec[TickerData] = DeriveJsonCodec.gen

  def apply(): Http[HistoricalDataRepo, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req@Method.POST -> !! / "histDT" =>
        for {
          processedReq <- req.body.asString.map(_.fromJson[HistoricalDataRequestDT])
          result <- processedReq match {
            case Left(error) =>
              ZIO.debug(s"Error: $error").as(Response(status = Status.BadRequest, body = Body.fromString(s"$error")))
            case Right(historicalDataRequestDT: HistoricalDataRequestDT) =>
              val returnJson = req.header(Header.Accept).exists(_.mimeTypes.exists(_.mediaType == MediaType.application.json))
              val from = LocalDateTimeUtil.toEpoch(historicalDataRequestDT.from)
              val to = LocalDateTimeUtil.toEpoch(historicalDataRequestDT.to)
              HistoricalDataRepo.getHistoricalData(historicalDataRequestDT.ticker, from, to).map { data =>
                if (returnJson) {
                  Response.json(data.toJson)
                } else {
                  Response(body = Body.fromStream(ZStream.fromIterable(SerializeTOHLCV.serialize(data))),
                    status = Status.Ok)
                }
              }.catchAll(error)
          }
        } yield result
      case req@Method.POST -> !! / "hist" =>
        for {
          processedReq <- req.body.asString.map(_.fromJson[HistoricalDataRequest])
          result <- processedReq match {
            case Left(error) =>
              ZIO.debug(s"Error: $error").as(Response(status = Status.BadRequest, body = Body.fromString(s"$error")))
            case Right(historicalDataRequest: HistoricalDataRequest) =>
              val returnJson = req.header(Header.Accept).exists(_.mimeTypes.exists(_.mediaType == MediaType.application.json))
              HistoricalDataRepo.getHistoricalData(historicalDataRequest.ticker, historicalDataRequest.from, historicalDataRequest.to).map { data =>
                if (returnJson) {
                  Response.json(data.toJson)
                } else {
                  Response(body = Body.fromStream(ZStream.fromIterable(SerializeTOHLCV.serialize(data))),
                    status = Status.Ok)
                }
              }.catchAll(error)
          }
        } yield result
    }
}

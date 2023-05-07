package serv1.backend.dto

import serv1.backend.json.JsonCommon
import zio.json.{DeriveJsonCodec, JsonCodec}

case class TickerTypeRequest(id: Option[Int], name: Option[String], exchange: Option[String])

object TickerTypeRequest extends JsonCommon {
  implicit val tickerTypeRequest: JsonCodec[TickerTypeRequest] = DeriveJsonCodec.gen
}

package serv1.backend.dto

import serv1.backend.json.JsonCommon
import serv1.model.ticker.TickerLoadType
import zio.json.{DeriveJsonCodec, JsonCodec}

case class HistoricalDataRequest(ticker: TickerLoadType, from: Long, to: Long)

object HistoricalDataRequest extends JsonCommon {
  implicit val historicalDataRequestCodec: JsonCodec[HistoricalDataRequest] = DeriveJsonCodec.gen
}
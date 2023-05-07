package serv1.backend.dto

import serv1.backend.json.JsonCommon
import serv1.model.ticker.TickerLoadType
import zio.json._

import java.time.LocalDateTime

case class HistoricalDataRequestDT(ticker: TickerLoadType, from: LocalDateTime, to: LocalDateTime)

object HistoricalDataRequestDT extends JsonCommon {
  implicit val historicalDataRequestCodec: JsonCodec[HistoricalDataRequestDT] = DeriveJsonCodec.gen
}

package serv1.backend.json

import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}
import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}

trait JsonCommon {
  implicit val barSizeDecoder: JsonDecoder[BarSizes.BarSize] = JsonDecoder[String].map { s => BarSizes.withName(s) }
  implicit val barSizeEncoder: JsonEncoder[BarSizes.BarSize] = JsonEncoder[String].contramap[BarSizes.BarSize](_.toString)
  implicit val tickerTypeCodec: JsonCodec[TickerType] = DeriveJsonCodec.gen
  implicit val tickerLoadTypeCodec: JsonCodec[TickerLoadType] = DeriveJsonCodec.gen
}

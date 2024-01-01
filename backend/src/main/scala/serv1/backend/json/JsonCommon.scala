package serv1.backend.json

import serv1.db.types.HistoricalDataType
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}
import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}

trait JsonCommon {
  implicit val barSizeDecoder: JsonDecoder[BarSizes.BarSize] = JsonDecoder[String].map { s => BarSizes.withName(s) }
  implicit val barSizeEncoder: JsonEncoder[BarSizes.BarSize] = JsonEncoder[String].contramap[BarSizes.BarSize](_.toString)
  implicit val tickerTypeCodec: JsonCodec[TickerType] = DeriveJsonCodec.gen
  implicit val tickerLoadTypeCodec: JsonCodec[TickerLoadType] = DeriveJsonCodec.gen
  implicit val historicalDataTypeCodec: JsonCodec[HistoricalDataType] = JsonCodec[HistoricalDataType](
    JsonEncoder[String].contramap[HistoricalDataType] { historicalDataType: HistoricalDataType =>
      historicalDataType.toString
    },
    JsonDecoder[String].mapOrFail(name =>
      try {
        Right(HistoricalDataType.withName(name))
      } catch {
        case error: NoSuchElementException => Left(error.getMessage)
      })
  )
}

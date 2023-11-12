package serv1.db.schema

import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.HistoricalData

import scala.language.implicitConversions

case class TickerData(id: Int, time: Long, open: Long, high: Long, low: Long, close: Long, vol: Double, historicalDataType: HistoricalDataType)

object TickerData {
  implicit def historicalDataToTickerData(historicalData: HistoricalData): TickerData = {
    TickerData(0, historicalData.timestamp, historicalData.open, historicalData.high, historicalData.low, historicalData.close, historicalData.vol, historicalData.historicalDataType)
  }

  implicit def tickerDataToHistoricalData(tickerData: TickerData): HistoricalData = {
    HistoricalData(tickerData.time, tickerData.high, tickerData.low, tickerData.open, tickerData.close, tickerData.vol, tickerData.historicalDataType)
  }
}

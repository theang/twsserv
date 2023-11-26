package serv1.model.ticker

import serv1.db.types.HistoricalDataType.HistoricalDataType

case class TickerLoadType(tickerType: TickerType, barSize: BarSizes.BarSize, historicalDataType: HistoricalDataType)

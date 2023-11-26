package serv1.model

import serv1.db.types.HistoricalDataType.HistoricalDataType

case class HistoricalData(
                           var timestamp: Long,
                           var high: Double,
                           var low: Double,
                           var open: Double,
                           var close: Double,
                           var vol: Double,
                           var historicalDataType: HistoricalDataType
                         )

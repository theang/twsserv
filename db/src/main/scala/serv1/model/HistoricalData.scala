package serv1.model

import serv1.db.types.HistoricalDataType.HistoricalDataType

case class HistoricalData(
                           var timestamp: Long,
                           var high: Long,
                           var low: Long,
                           var open: Long,
                           var close: Long,
                           var vol: Double,
                           var historicalDataType: HistoricalDataType
                         )

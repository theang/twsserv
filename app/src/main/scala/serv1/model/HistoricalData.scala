package serv1.model

case class HistoricalData(
                           var timestamp: Long,
                           var high: Long,
                           var low: Long,
                           var open: Long,
                           var close: Long,
                           var vol: Double
                         )

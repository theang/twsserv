package serv1.db.schema

case class TickerTickBidAsk(id: Long, time: Long, bidPrice: Double, askPrice: Double, bidSize: Double, askSize: Double, bidPastLow: Boolean, askPastHigh: Boolean)

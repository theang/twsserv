package serv1.db.schema

case class TickerTickBidAsk(id: Long, time: Long, nanoTime: Long, bidPrice: Double, askPrice: Double, bidSize: Double, askSize: Double, bidPastLow: Boolean, askPastHigh: Boolean) extends TickerTickTimed

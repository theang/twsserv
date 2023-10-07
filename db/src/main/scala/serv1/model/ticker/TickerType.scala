package serv1.model.ticker

case class TickerType(name: String, exchange: String, typ: String, prec: Int, localSymbol: Option[String],
                      strike: Option[Int], right: Option[String], multiplier: Option[String],
                      lastTradeDateOrContractMonth: Option[String], currency: Option[String], primaryExchange: Option[String])

package serv1.client.model

import serv1.db.schema.TickerTickLast

case class TickerTickLastExchange(tickerTick: TickerTickLast, exch: String)
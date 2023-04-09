package serv1.db.schema

case class TickerTickLast(id: Long, time: Long, price: Double, size: Double, exch: Int, spec: String, pastLimit: Boolean, unreported: Boolean)

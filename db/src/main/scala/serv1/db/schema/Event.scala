package serv1.db.schema

case class Event(id: Int, typ: String, symbol: String, time: Long, info: String)

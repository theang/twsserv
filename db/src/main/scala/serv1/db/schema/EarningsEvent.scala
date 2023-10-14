package serv1.db.schema

case class EarningsEvent(id: Int, eventId: Int, forecast: Boolean, fiscalQuarterEnding: String, eps: Option[Double], epsForecast: Option[Double], marketCap: Option[Double], lastYearEps: Option[Double], lastYearDate: Option[Long])

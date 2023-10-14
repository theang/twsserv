package serv1.model.event

case class EarningsEventType(symbol: String, time: Long, info: String, forecast: Boolean, fiscalQuarterEnding: String, eps: Option[Double], epsForecast: Option[Double], marketCap: Option[Double], lastYearEps: Option[Double], lastYearDate: Option[Long])

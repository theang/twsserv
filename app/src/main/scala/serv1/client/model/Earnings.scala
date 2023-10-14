package serv1.client.model

case class Earnings(symbol: String, info: String, forecast: Boolean, fiscalQuarterEnding: String, eps: Option[Double], epsForecast: Option[Double], marketCap: Option[Double], lastYearEps: Option[Double], lastYearDate: Option[Long])

package serv1.db.schema

import serv1.db.types.EarningsTimeType.EarningsTimeType

case class EarningsEvent(id: Int,
                         eventId: Int,
                         forecast: Boolean,
                         fiscalQuarterEnding: String,
                         eps: Option[Double],
                         epsForecast: Option[Double],
                         marketCap: Option[Double],
                         lastYearEps: Option[Double],
                         lastYearDate: Option[Long],
                         time: Option[EarningsTimeType])

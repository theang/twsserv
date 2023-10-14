package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object EarningsEventTable {
  val query = TableQuery[EarningsEventTable]
}

class EarningsEventTable(tag: Tag) extends Table[EarningsEvent](tag, _tableName = "EARNINGS_EVENT") {
  def id = column[Int]("ID", O.PrimaryKey, O.Unique, O.AutoInc)

  def eventId = column[Int]("EVENT_ID")

  def forecast = column[Boolean]("FORECAST")

  def fiscalQuarterEnding = column[String]("FISCAL_QUARTER_ENDING")

  def eps = column[Option[Double]]("EPS")

  def epsForecast = column[Option[Double]]("EPS_FORECAST")

  def marketCap = column[Option[Double]]("MARKET_CAP")

  def lastYearEps = column[Option[Double]]("LAST_YEAR_EPS")

  def lastYearDate = column[Option[Long]]("LAST_YEAR_DATE")

  def * = (id, eventId, forecast, fiscalQuarterEnding, eps, epsForecast, marketCap, lastYearEps, lastYearDate) <> (EarningsEvent.tupled, EarningsEvent.unapply)

  def fiscalQuarterIndex = index("EARNINGS_EVENT_FISCAL_QUARTER_INDEX", fiscalQuarterEnding)

  def eventIdIndex = index("EARNINGS_EVENT_EVENT_ID_INDEX", eventId, unique = true)
}

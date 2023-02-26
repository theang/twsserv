package serv1.db.schema

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

object TickerTrackingTable {
  val query = TableQuery[TickerTrackingTable]
}

class TickerTrackingTable(tag: Tag) extends Table[TickerTracking](tag, "TICKER_TRACKING") {
  def id = column[Int]("ID", O.PrimaryKey, O.Unique)

  def tickerType = column[Int]("TICKER_TYPE_ID")

  def schedule = column[Int]("SCHEDULE_ID")

  def * = (id, tickerType, schedule) <> (TickerTracking.tupled, TickerTracking.unapply)

  def tickerTypeFK =
    foreignKey("TICKER_TRACKING_TICKER_TYPE_ID_FK",
      tickerType,
      TickerTypeTable.query)(_.id, onDelete = ForeignKeyAction.Cascade)

  def schedulerFK =
    foreignKey("TICKER_TRACKING_SCHEDULED_TASK_FK",
      schedule,
      ScheduledTaskTable.query)(_.id, onDelete = ForeignKeyAction.Cascade)

  def tickerTypeScheduleIndex = index("IND_TICKER_TYPE_SCHEDULE_TICKER_TRACKING_TABLE", (tickerType, schedule), unique = true)
}
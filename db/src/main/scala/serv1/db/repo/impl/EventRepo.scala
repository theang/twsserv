package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.repo.intf.EventRepoIntf
import serv1.db.schema.{EarningsEvent, EarningsEventTable, Event, EventTable}
import serv1.db.types.EarningsTimeType
import serv1.model.event.EarningsEventType
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._
import slick.lifted.CanBeQueryCondition
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.language.{existentials, postfixOps}

object EventRepo extends EventRepoIntf with Logging {
  implicit var duration: Duration.Infinite = Duration.Inf
  val EARNINGS_EVENT_TYPE = "EARNINGS_EVENT"

  def insertEarningsEvent(event: EarningsEventType): DBIOAction[Unit, NoStream, Effect.Read with Effect.Write with Effect.Transactional]
  = {
    val updateTime = event.earningsTime.exists(_ != EarningsTimeType.TIME_NOT_SUPPLIED)
    val eventQuery = EventTable.query
    val earningsEventQuery = EarningsEventTable.query
    (for {
      existingEvent <- eventQuery.filter(ev => ev.time === event.time && ev.typ === EARNINGS_EVENT_TYPE && ev.symbol === event.symbol).result.headOption
      updatedEvent = existingEvent.map(_.copy(info = event.info)) getOrElse Event(0, EARNINGS_EVENT_TYPE, event.symbol, event.time, event.info)
      result <- (eventQuery returning eventQuery.map(_.id)).insertOrUpdate(updatedEvent)
      eventId = if (result.isEmpty) existingEvent.get.id else result.get
      existingEarningsEvent <- EarningsEventTable.query.filter(eev => eev.eventId === eventId).result.headOption
      earningsEvent = existingEarningsEvent.map { v =>
        v.copy(forecast = event.forecast,
          fiscalQuarterEnding = event.fiscalQuarterEnding,
          eps = event.eps,
          epsForecast = event.epsForecast,
          marketCap = event.marketCap,
          lastYearEps = event.lastYearEps,
          lastYearDate = event.lastYearDate,
          time = if (updateTime) event.earningsTime else v.time)
      } getOrElse EarningsEvent(0, eventId, event.forecast,
        event.fiscalQuarterEnding,
        event.eps,
        event.epsForecast,
        event.marketCap,
        event.lastYearEps,
        event.lastYearDate,
        event.earningsTime)
      _ <- earningsEventQuery.insertOrUpdate(earningsEvent)
    } yield ()).transactionally
  }

  override def insertEarningsEvents(events: Seq[EarningsEventType]): Unit = {
    Await.result(DB.db.run(DBIO.sequence(events.map(insertEarningsEvent))), duration)
  }

  def mapEarningsEventResult(result: (EventTable#TableElementType, EarningsEventTable#TableElementType)): EarningsEventType = {
    result match {
      case (event: Event, earningsEvent: EarningsEvent) => EarningsEventType(event.symbol, event.time, event.info,
        earningsEvent.forecast, earningsEvent.fiscalQuarterEnding, earningsEvent.eps, earningsEvent.epsForecast,
        earningsEvent.marketCap, earningsEvent.lastYearEps, earningsEvent.lastYearDate, earningsEvent.time)
    }
  }

  def getEarningsEventsByFilter[T](filter: ((EventTable, EarningsEventTable)) => T)(implicit wt: CanBeQueryCondition[T]): Seq[EarningsEventType] = {
    val action = for {
      result <- (EventTable.query join EarningsEventTable.query
        on (_.id === _.eventId)).filter(filter).result
    } yield result.map(mapEarningsEventResult)
    Await.result(DB.db.run(action), duration)
  }

  override def getEarningsEventsByDate(date: Long): Seq[EarningsEventType] = {
    getEarningsEventsByFilter(ev => ev._1.time === date && ev._1.typ === EARNINGS_EVENT_TYPE)
  }

  override def getEarningsEventsFromDate(date: Long): Seq[EarningsEventType] = {
    getEarningsEventsByFilter(ev => ev._1.time >= date && ev._1.typ === EARNINGS_EVENT_TYPE)
  }

  override def getEarningsEventsBySymbol(symbol: String): Seq[EarningsEventType] = {
    getEarningsEventsByFilter(ev => ev._1.symbol === symbol && ev._1.typ === EARNINGS_EVENT_TYPE)
  }

  override def deleteEarningEventsByDate(date: Long): Unit = {
    val eventQuery = EventTable.query
    val earningsEventQuery = EarningsEventTable.query
    val deleteEventsAction = eventQuery.filter(ev => ev.time === date && ev.typ === EARNINGS_EVENT_TYPE)
    val deleteEarningsEventsAction = earningsEventQuery.filter(_.eventId in deleteEventsAction.map(_.id))
    Await.result(DB.db.run(DBIOAction.seq(deleteEarningsEventsAction.delete, deleteEventsAction.delete).transactionally), duration)
  }

  override def deleteEarningEventsBySymbol(symbol: String): Unit = {
    val eventQuery = EventTable.query
    val earningsEventQuery = EarningsEventTable.query
    val deleteEventsAction = eventQuery.filter(ev => ev.symbol === symbol && ev.typ === EARNINGS_EVENT_TYPE)
    val deleteEarningsEventsAction = earningsEventQuery.filter(_.eventId in deleteEventsAction.map(_.id))
    Await.result(DB.db.run(DBIOAction.seq(deleteEarningsEventsAction.delete, deleteEventsAction.delete).transactionally), duration)
  }
}

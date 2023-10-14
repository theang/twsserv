package serv1.db.repo.intf

import serv1.model.event.EarningsEventType

trait EventRepoIntf {
  def insertEarningsEvents(events: Seq[EarningsEventType]): Unit

  def getEarningsEventsByDate(date: Long): Seq[EarningsEventType]

  def getEarningsEventsFromDate(date: Long): Seq[EarningsEventType]

  def getEarningsEventsBySymbol(symbol: String): Seq[EarningsEventType]

  def deleteEarningEventsByDate(date: Long): Unit

  def deleteEarningEventsBySymbol(symbol: String): Unit
}

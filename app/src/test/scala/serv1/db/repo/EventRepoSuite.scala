package serv1.db.repo

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.TestData
import serv1.db.repo.impl.EventRepo
import serv1.db.repo.intf.EventRepoIntf
import serv1.model.event.EarningsEventType
import slick.util.Logging

@RunWith(classOf[JUnitRunner])
class EventRepoSuite extends AnyFunSuite with Logging {
  def createEarningsEvent(date: Long): EarningsEventType = EarningsEventType(TestData.xomTicker, date, "", forecast = true, "", Some(1.0), Some(1), Some(1), Some(1), Some(1))

  test("EventRepo test, create event, update event, check event") {
    val repo: EventRepoIntf = EventRepo
    assert(repo.getEarningsEventsByDate(0).size === 0)
    val event = createEarningsEvent(0)
    repo.insertEarningsEvents(Seq(event))
    var earnings = repo.getEarningsEventsByDate(0)
    assert(earnings.size === 1)
    repo.deleteEarningEventsByDate(0)
    earnings = repo.getEarningsEventsByDate(0)
    assert(earnings.size === 0)

    repo.insertEarningsEvents(Seq(event))
    earnings = repo.getEarningsEventsByDate(0)
    assert(earnings.size === 1)
    repo.deleteEarningEventsBySymbol(TestData.xomTicker)
    earnings = repo.getEarningsEventsByDate(0)
    assert(earnings.size === 0)
  }
}

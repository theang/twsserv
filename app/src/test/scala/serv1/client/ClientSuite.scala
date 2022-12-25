package serv1.client

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.client.converters.BarSizeConverter
import serv1.db.TestData
import serv1.model.HistoricalData
import serv1.model.ticker.BarSizes
import serv1.util.LocalDateTimeUtil

@RunWith(classOf[JUnitRunner])
class ClientSuite extends AnyFunSuite {
  test("data client") {
    var monitor: Object = new Object
    var done: Boolean = false
    var error: Boolean = false

    TWSClient.loadHistoricalData(LocalDateTimeUtil.toEpoch(TestData.from), LocalDateTimeUtil.toEpoch(TestData.to),
      TestData.xomTicker, TestData.xomTickerExch, TestData.xomTickerType, BarSizeConverter.getBarSizeSeconds(BarSizes.HOUR), 2,
      (data: List[HistoricalData], last: Boolean) => {
        monitor.synchronized {
          if (last) {
            done = true
            monitor.notify()
          }
        }
      }, (code: Int, msg: String) => {
        monitor.synchronized {
          error = true
          monitor.notify()
        }
      })
    monitor.synchronized {
      monitor.wait(5000)
    }
    assert(done === true)
    assert(error === false)
  }
}

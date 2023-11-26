package serv1.client

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.client.converters.BarSizeConverter
import serv1.db.TestData
import serv1.db.types.HistoricalDataType
import serv1.model.HistoricalData
import serv1.model.ticker.{BarSizes, TickerType}
import serv1.util.LocalDateTimeUtil

@RunWith(classOf[JUnitRunner])
class MultiClientSuite extends AnyFunSuite {
  test("Yahoo data client") {
    var monitor: Object = new Object
    var hData: List[HistoricalData] = null
    var done: Boolean = false
    var error: Boolean = false

    YahooClient.loadHistoricalData(LocalDateTimeUtil.toEpoch(TestData.from), LocalDateTimeUtil.toEpoch(TestData.to),
      TickerType(TestData.xomTicker, TestData.xomTickerExch, TestData.xomTickerType, 2, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty), BarSizeConverter.getBarSizeSeconds(BarSizes.DAY),
      HistoricalDataType.TRADES,
      (data: Seq[HistoricalData], last: Boolean) => {
        monitor.synchronized {
          if (last) {
            hData = data.toList
            done = true
            monitor.notify()
          }
        }
      }, (code: Int, msg: String, rejectedJson: String) => {
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
    assert(hData.size === 3)
  }
}

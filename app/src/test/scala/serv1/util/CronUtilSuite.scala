package serv1.util

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

import java.time.LocalDateTime

@RunWith(classOf[JUnitRunner])
class CronUtilSuite extends AnyFunSuite {
  test("Cron util scheduling tests") {
    val startEpoch: Long = LocalDateTimeUtil.toEpoch(LocalDateTime.of(2023, 1, 19, 23, 1))
    val toEpoch: Long = LocalDateTimeUtil.toEpoch(LocalDateTime.of(2023, 1, 20, 23, 0))
    for (i <- startEpoch to toEpoch) {
      val nextRun: Long = CronUtil.findNextRun(i,
        "0 23 * * *")
      val ldt: LocalDateTime = LocalDateTimeUtil.fromEpoch(nextRun)
      assert(ldt.getYear === 2023)
      assert(ldt.getMonth.getValue === 1)
      assert(ldt.getDayOfMonth === 20)
      assert(ldt.getHour === 23)
      assert(ldt.getMinute === 0)
    }
  }
}

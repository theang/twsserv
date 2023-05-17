package serv1.time

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HighResTimeSuite extends AnyFunSuite {
  test("HighResTime test") {
    val nano = HighResTime.currentNanos

  }
}

package serv1.db

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.schema.TickerDataTableNameUtil
import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}

@RunWith(classOf[JUnitRunner])
class DBUtilSuite extends AnyFunSuite {
  val tickerLoadType: TickerLoadType = TickerLoadType(TickerType("TEST", "EXC", "STK", 2, Option.empty, Option.empty, Option.empty,
    Option.empty, Option.empty, Option.empty, Option.empty), BarSizes.MIN15)
  test("test ticket data table name parsing format") {
    val tableName = TickerDataTableNameUtil.formatTableName(tickerLoadType)
    val testTickerLoadType = TickerDataTableNameUtil.parseTableName(tableName).get
    assert(tickerLoadType == testTickerLoadType)
  }
  test("test failed ticker data table parsing") {
    val tableName1 = "TD_EXC_STK_TEST_MIN15"
    val tableName2 = "TD_EXC_STK_TEST_MIN25"
    val tableName3 = "TD_EXC_STK_TEST_SOME"
    val tableName4 = "TD_EXC_STK_TEST_SOME_WER"
    assert {
      TickerDataTableNameUtil.parseTableName(tableName1).get == tickerLoadType
      TickerDataTableNameUtil.parseTableName(tableName2).isEmpty
      TickerDataTableNameUtil.parseTableName(tableName3).isEmpty
      TickerDataTableNameUtil.parseTableName(tableName4).isEmpty
    }
  }
}

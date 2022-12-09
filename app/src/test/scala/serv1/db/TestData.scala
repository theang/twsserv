package serv1.db

import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}

import java.time.LocalDateTime
import java.util.UUID

object TestData {
  val TestID = UUID.randomUUID()
  val testTicker = TickerLoadType(TickerType("TEST", "EXC", "STK", 2), BarSizes.DAY)
  val testTicker2 = TickerLoadType(TickerType("TEST2", "EXC", "STK", 2), BarSizes.MIN15)
  val testTickers = List(testTicker, testTicker2)
  val from = LocalDateTime.of(2022, 12, 3, 12, 0)
  val to = LocalDateTime.of(2022, 12, 5, 12, 0)
}

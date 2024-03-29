package serv1.db

import serv1.db.schema.TickerTypeDB
import serv1.db.types.HistoricalDataType
import serv1.model.HistoricalData
import serv1.model.job.{JobStatuses, TickLoadingJobState, TickerJobState}
import serv1.model.ticker.BarSizes.BarSize
import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}
import serv1.util.LocalDateTimeUtil

import java.time.LocalDateTime
import java.util.UUID

object TestData {
  val TestID: UUID = UUID.randomUUID()
  val xomTicker: String = "XOM"
  val xomTickerType: String = "STK"
  val xomTickerExch: String = "NYSE"
  //val xomTickerExch: String = "SMART"
  def createTickerTypeDB(id: Int, name: String, exchange: String, typ: String, barSize: BarSize, prec: Int, localSymbol: Option[String] = Option.empty,
                         strike: Option[Int] = Option.empty, right: Option[String] = Option.empty, multiplier: Option[String] = Option.empty,
                         lastTradeDateOrContractMonth: Option[String] = Option.empty, currency: Option[String] = Option.empty, primaryExchange: Option[String] = Option.empty): TickerTypeDB = TickerTypeDB(id, name, exchange, typ, barSize, prec, localSymbol, strike, right, multiplier, lastTradeDateOrContractMonth, currency, primaryExchange)

  def createTickerType(name: String, exchange: String, typ: String, prec: Int, localSymbol: Option[String] = Option.empty,
                       strike: Option[Int] = Option.empty, right: Option[String] = Option.empty, multiplier: Option[String] = Option.empty,
                       lastTradeDateOrContractMonth: Option[String] = Option.empty, currency: Option[String] = Option.empty, primaryExchange: Option[String] = Option.empty): TickerType = TickerType(name, exchange, typ, prec, localSymbol, strike, right, multiplier, lastTradeDateOrContractMonth, currency, primaryExchange)

  val testTicker: TickerLoadType = TickerLoadType(TickerType("TEST", "EXC", "STK", 2, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty), BarSizes.DAY, HistoricalDataType.TRADES)
  val testTicker2: TickerLoadType = TickerLoadType(TickerType("TEST2", "EXC", "STK", 2, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty), BarSizes.MIN15, HistoricalDataType.TRADES)
  val testTickers: List[TickerLoadType] = List(testTicker, testTicker2)
  val from: LocalDateTime = LocalDateTime.of(2022, 12, 6, 12, 0)
  val to: LocalDateTime = LocalDateTime.of(2022, 12, 9, 12, 0)
  val testHistoricalData: HistoricalData = HistoricalData(LocalDateTimeUtil.toEpoch(from), 2000, 1000, 1500, 1600, 1000, HistoricalDataType.TRADES)
  val testHistoricalData1: HistoricalData = HistoricalData(LocalDateTimeUtil.toEpoch(from) + 60 * 60 * 24, 2100, 1100, 1600, 1700, 1100, HistoricalDataType.TRADES)
  val testHistoricalData2: HistoricalData = HistoricalData(LocalDateTimeUtil.toEpoch(to), 2050, 1050, 1550, 1650, 1050, HistoricalDataType.TRADES)
  val testTickerJobState: TickerJobState = TickerJobState(JobStatuses.IN_PROGRESS,
    tickers = testTickers, loadedTickers = List.empty, errors = List.empty, ignoredTickers = List.empty, from = from, to = to, overwrite = true)
  val testTickerJobStateFinished: TickerJobState = testTickerJobState.copy(status = JobStatuses.FINISHED)
  val testTickerTickLoadingJobState: TickLoadingJobState = TickLoadingJobState(JobStatuses.IN_PROGRESS,
    tickers = testTickers, errors = List.empty)

  val testScheduleName: String = "test"
  val testSchedule: String = "* * * * *"
  val testScheduleRun: Long = LocalDateTimeUtil.toEpoch(from)

  val testScheduleName1: String = "newTest"
  val testSchedule1: String = "10-20/2 * * * *"
  val testScheduleRun1: Long = LocalDateTimeUtil.toEpoch(from) + 1000
}

package serv1.db

import akka.http.scaladsl.model.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.repo.TickerDataRepo
import serv1.db.schema.{Job, JobTable, TickerData, TickerDataTable, TickerDataTableGen, TickerTypeDB, TickerTypeTable}
import serv1.job.TickerJobState
import serv1.model.HistoricalData
import serv1.model.job.JobStatuses
import serv1.model.ticker.{BarSizes, TickerLoadType, TickerType}
import serv1.rest.JsonFormats
import serv1.rest.loaddata.LoadService
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Await
import spray.json._

import scala.language.implicitConversions
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class DBSuite extends AnyFunSuite with JsonFormats {
  val TestID = UUID.randomUUID()
  val testTicker = TickerLoadType(TickerType("TEST", "EXC", "STK", 2), BarSizes.DAY)
  val testTicker2 = TickerLoadType(TickerType("TEST2", "EXC", "STK", 2), BarSizes.MIN15)
  val testTickers = List(testTicker, testTicker2)
  val from = LocalDateTime.of(2022, 12, 3, 12, 0)
  val to = LocalDateTime.of(2022, 12, 5, 12, 0)

  test("try creating job record") {
    DB.createTables()
    val job = Job(TestID,
      new TickerJobState(JobStatuses.IN_PROGRESS,
        tickers = testTickers, List.empty, from, to).toJson.prettyPrint
    )
    DB.db.run(DBIO.seq(JobTable.query += job))

    val testJob: Job = Await.result(
      DB.db.run(JobTable.query.filter(_.jobId === TestID).take(1).result.head),
      Duration.Inf)

    assert(testJob == job)

    Await.result(DB.db.run(JobTable.query.filter(_.jobId === TestID).delete), Duration.Inf)
  }

  test("try creating ticker type record") {
    DB.createTables()
    val ticker = TickerTypeDB(0, "TEST", "EXC", "STK", BarSizes.MIN15, 2)
    val findTicker = (e:TickerTypeTable) => e.name === "TEST" && e.exchange === "EXC" && e.typ === "STK" && e.barSize === BarSizes.MIN15
    Await.result(DB.db.run(TickerTypeTable.query.filter(findTicker).delete), Duration.Inf)

    // given
    Await.result(DB.db.run(DBIO.seq(TickerTypeTable.query += ticker)), Duration.Inf)

    val testTicker: TickerTypeDB = Await.result(
      DB.db.run(TickerTypeTable.query.filter(findTicker).take(1).result.head),
      Duration.Inf)

    val tickerType: TickerLoadType = ticker
    val testTickerType: TickerLoadType = testTicker
    // then
    assert(testTickerType == tickerType)

    Await.result(DB.db.run(TickerTypeTable.query.delete), Duration.Inf)
  }

  test ("try creating ticker data record") {
    val ticker = TickerTypeDB(0, "TEST", "EXC", "STK", BarSizes.MIN15, 2)
    val tickerType: TickerLoadType = ticker
    TickerDataRepo.createTableIfNotExists(tickerType)
    val tickerDataTable = TickerDataTable.getQuery(tickerType)
    val clazz = new TickerDataTableGen(tickerType)
    val query = TableQuery[clazz.TickerDataTable]
    Await.result(DB.db.run(query.filter(td=>td.time === 1000L).delete), Duration.Inf)
    val historicalData = HistoricalData(1000, 2000, 1000, 1500, 1600, 1000)
    Await.result(DB.db.run(tickerDataTable += historicalData), Duration.Inf)
    val testTickerData = Await.result(DB.db.run(query.filter(td=>td.time === 1000L).result.head), Duration.Inf)
    val testHistoricalData:HistoricalData = testTickerData
    Await.result(DB.db.run(query.filter(td=>td.time === 1000L).delete), Duration.Inf)
    assert(historicalData == testHistoricalData)
  }
}

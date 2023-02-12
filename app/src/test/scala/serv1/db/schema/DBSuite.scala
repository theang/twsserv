package serv1.db.schema

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.DB
import serv1.db.TestData._
import serv1.db.repo.impl.TickerDataRepo
import serv1.job.TickerJobState
import serv1.model.HistoricalData
import serv1.model.job.JobStatuses
import serv1.model.ticker.{BarSizes, TickerLoadType}
import serv1.rest.JsonFormats
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class DBSuite extends AnyFunSuite with JsonFormats {
  test("try creating job record") {
    DB.createTables()
    val job = Job(TestID,
      TickerJobState(JobStatuses.IN_PROGRESS,
        tickers = testTickers, List.empty, List.empty, from, to).toJson.prettyPrint
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
    val findTicker = (e: TickerTypeTable) => e.name === "TEST" && e.exchange === "EXC" && e.typ === "STK" && e.barSize === BarSizes.MIN15
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

  test("try creating ticker data record") {
    DB.createTables()
    val ticker = TickerTypeDB(0, "TEST", "EXC", "STK", BarSizes.MIN15, 2)
    val tickerType: TickerLoadType = ticker
    TickerDataRepo.createTableIfNotExists(tickerType)
    val tickerDataTable = TickerDataTable.getQuery(tickerType)
    val clazz = new TickerDataTableGen(tickerType)
    val query = TableQuery[clazz.TickerDataTable]
    Await.result(DB.db.run(query.filter(td => td.time === 1000L).delete), Duration.Inf)
    val historicalData = HistoricalData(1000, 2000, 1000, 1500, 1600, 1000)
    Await.result(DB.db.run(tickerDataTable += historicalData), Duration.Inf)
    val testTickerData = Await.result(DB.db.run(query.filter(td => td.time === 1000L).result.head), Duration.Inf)
    val testHistoricalData: HistoricalData = testTickerData
    Await.result(DB.db.run(query.filter(td => td.time === 1000L).delete), Duration.Inf)
    assert(historicalData == testHistoricalData)
  }
}

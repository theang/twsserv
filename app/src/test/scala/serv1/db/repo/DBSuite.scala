package serv1.db.repo

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.TestData.{TestID, testTickers}
import serv1.db.repo.impl.{ConfigRepo, TickerDataRepo}
import serv1.db.schema._
import serv1.db.{Configuration, DB, DBJsonFormats, TestData}
import serv1.model.HistoricalData
import serv1.model.job.{JobStatuses, TickerJobState}
import serv1.model.ticker.{BarSizes, TickerLoadType}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{SQLActionBuilder, SetParameter}
import slick.util.Logging
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class DBSuite extends AnyFunSuite with DBJsonFormats with Logging {
  test("database schema upgrade from 0 to 1") {
    val updateTable: String =
      """START TRANSACTION;
        |  DO $$
        |  DECLARE
        |   sch text := 'public';
        |   tbl text;
        |  BEGIN
        |  FOR tbl IN
        |    SELECT table_name FROM information_schema.tables
        |    WHERE  table_schema in (sch)
        |      AND  table_name  like 'TD_%'
        |  LOOP
        |  EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN "VOL" TYPE int8 USING "VOL"::int8', sch, tbl);
        |  END LOOP;
        |  END $$ LANGUAGE 'plpgsql';
        |  COMMIT;""".stripMargin
    val sqlUpdate = SQLActionBuilder(Seq(updateTable), SetParameter.SetUnit)
    Await.result(DB.db.run(sqlUpdate.as[Unit]), Duration.Inf)
    ConfigRepo.deleteConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "")))
    DB.createTables()
    val checkUpdate =
      """SELECT data_type FROM information_schema.columns
        |    WHERE  table_schema in ('public')
        |     and column_name  = 'VOL'
        |      AND  table_name  like 'TD_%'""".stripMargin
    val sqlCheckUpdate = SQLActionBuilder(Seq(checkUpdate), SetParameter.SetUnit).as[String]
    val columnType: String = Await.result(DB.db.run(sqlCheckUpdate), Duration.Inf).head
    assert(columnType == "double precision")
  }
  test("try creating job record") {
    DB.createTables()
    val job = Job(TestID,
      TickerJobState(JobStatuses.IN_PROGRESS,
        tickers = testTickers, List.empty, List.empty, List.empty, TestData.from, TestData.to, overwrite = false).toJson.prettyPrint
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
package serv1.db.repo

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.TestData.{TestID, testTickers}
import serv1.db.repo.impl.{ConfigRepo, ExchangeRepo, TickerDataRepo, TickerTickRepo}
import serv1.db.schema._
import serv1.db.types.HistoricalDataType
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
  ignore("database schema upgrade from 0 to 1") {
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
        |      AND NOT (table_name  like 'TD_%_BIDASK')
        |      AND NOT (table_name  like 'TD_%_LAST')
        |  LOOP
        |  EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN "VOL" TYPE int8 USING "VOL"::int8', sch, tbl);
        |  END LOOP;
        |  END $$ LANGUAGE 'plpgsql';
        |  COMMIT;""".stripMargin
    val sqlUpdate = SQLActionBuilder(Seq(updateTable), SetParameter.SetUnit)
    Await.result(DB.db.run(sqlUpdate.as[Unit]), Duration.Inf)
    ConfigRepo.deleteConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "")))
    DB.createTables("1")
    val checkUpdate =
      """SELECT data_type FROM information_schema.columns
        |    WHERE  table_schema in ('public')
        |     and column_name  = 'VOL'
        |      AND  table_name  like 'TD_%'
        |      AND NOT (table_name  like 'TD_%_BIDASK')
        |      AND NOT (table_name  like 'TD_%_LAST')
        |      """.stripMargin
    val sqlCheckUpdate = SQLActionBuilder(Seq(checkUpdate), SetParameter.SetUnit).as[String]
    val columnType: String = Await.result(DB.db.run(sqlCheckUpdate), Duration.Inf).head
    assert(columnType == "double precision")
  }
  ignore("database schema upgrade from 1 to 2") {
    val updateTable: String =
      """START TRANSACTION;
        |  DO $$
        |  DECLARE
        |   sch text := 'public';
        |   tbl text := 'TICKER';
        |   clm text;
        |  BEGIN
        |  FOR clm IN
        |    SELECT column_name FROM information_schema.columns
        |    WHERE  table_schema in (sch)
        |      AND  table_name  in (tbl)
        |      AND  column_name in ('LOCAL_SYMBOL', 'STRIKE', 'RIGHT', 'MULTIPLIER', 'LAST_TRADE_DATE_OR_CONTRACT_MONTH',
        |                           'MULTIPLIER', 'CURRENCY')
        |  LOOP
        |  EXECUTE format('ALTER TABLE %I.%I DROP COLUMN %I', sch, tbl, clm);
        |  END LOOP;
        |  DROP INDEX IF EXISTS "IND_NAME";
        |  CREATE INDEX "IND_NAME" ON "TICKER" ("NAME", "EXCHANGE", "TYP", "BAR_SIZE", "PREC");
        |  END $$ LANGUAGE 'plpgsql';
        |  COMMIT;""".stripMargin
    val sqlUpdate = SQLActionBuilder(Seq(updateTable), SetParameter.SetUnit)
    Await.result(DB.db.run(sqlUpdate.as[Unit]), Duration.Inf)
    ConfigRepo.deleteConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "")))
    ConfigRepo.putConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "1")))
    DB.createTables("2")
    val checkUpdate =
      """select string_agg(column_name, ',' order by column_name)
        | from information_schema.columns
        | where table_schema = 'public'
        |   and table_name = 'TICKER'""".stripMargin
    val sqlCheckUpdate = SQLActionBuilder(Seq(checkUpdate), SetParameter.SetUnit).as[String]
    val columns: String = Await.result(DB.db.run(sqlCheckUpdate), Duration.Inf).head
    assert(columns == "BAR_SIZE,CURRENCY,EXCHANGE,ID,LAST_TRADE_DATE_OR_CONTRACT_MONTH,LOCAL_SYMBOL,MULTIPLIER,NAME,PREC,RIGHT,STRIKE,TYP")
  }

  ignore("database schema upgrade from 2 to 3") {
    val updateTable: String =
      """START TRANSACTION;
        |  DO $$
        |  DECLARE
        |   sch text := 'public';
        |   tbl text := 'TICKER';
        |   clm text;
        |  BEGIN
        |  FOR clm IN
        |    SELECT column_name FROM information_schema.columns
        |    WHERE  table_schema in (sch)
        |      AND  table_name  in (tbl)
        |      AND  column_name in ('PRIMARY_EXCHANGE')
        |  LOOP
        |  EXECUTE format('ALTER TABLE %I.%I DROP COLUMN %I', sch, tbl, clm);
        |  END LOOP;
        |  DROP INDEX IF EXISTS "IND_NAME";
        |  CREATE INDEX "IND_NAME" ON "TICKER" ("NAME", "EXCHANGE", "TYP", "BAR_SIZE", "PREC", "LOCAL_SYMBOL", "STRIKE", "RIGHT", "MULTIPLIER", "LAST_TRADE_DATE_OR_CONTRACT_MONTH",
        |                           "MULTIPLIER", "CURRENCY");
        |  END $$ LANGUAGE 'plpgsql';
        |  COMMIT;""".stripMargin
    val sqlUpdate = SQLActionBuilder(Seq(updateTable), SetParameter.SetUnit)
    Await.result(DB.db.run(sqlUpdate.as[Unit]), Duration.Inf)
    ConfigRepo.deleteConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "")))
    ConfigRepo.putConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "2")))
    DB.createTables("3")
    val checkUpdate =
      """select string_agg(column_name, ',' order by column_name)
        | from information_schema.columns
        | where table_schema = 'public'
        |   and table_name = 'TICKER'""".stripMargin
    val sqlCheckUpdate = SQLActionBuilder(Seq(checkUpdate), SetParameter.SetUnit).as[String]
    val columns: String = Await.result(DB.db.run(sqlCheckUpdate), Duration.Inf).head
    assert(columns == "BAR_SIZE,CURRENCY,EXCHANGE,ID,LAST_TRADE_DATE_OR_CONTRACT_MONTH,LOCAL_SYMBOL,MULTIPLIER,NAME,PREC,PRIMARY_EXCHANGE,RIGHT,STRIKE,TYP")
  }

  test("database schema upgrade from 3 to 4") {
    val updateTable: String =
      """START TRANSACTION;
        |  DO $$
        |  DECLARE
        |   sch text := 'public';
        |   tbl text;
        |   clm text;
        |  BEGIN
        |  FOR tbl, clm IN
        |    SELECT table_name, column_name FROM information_schema.columns
        |    WHERE  table_schema in (sch)
        |      AND  table_name  like 'TD_%'
        |      and  table_name  not like 'TD_%_BIDASK'
        |      and  table_name  not like 'TD_%_LAST'
        |      and data_type = 'double precision'
        |      AND  column_name in ('OPEN','HIGH','LOW','CLOSE')
        |  LOOP
        |  EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN %I TYPE double precision USING (%I * 100)::int8', sch, tbl, clm, clm);
        |  END LOOP;
        |
        |  FOR tbl IN
        |    SELECT table_name FROM information_schema.tables
        |    WHERE  table_schema in (sch)
        |      AND  table_name  like 'TD_%'
        |      AND NOT (table_name  like 'TD_%_BIDASK')
        |      AND NOT (table_name  like 'TD_%_LAST')
        |  LOOP
        |  EXECUTE format('DROP INDEX IF EXISTS %I.%I', sch, 'IND_TYP_TIME_' || tbl);
        |  END LOOP;
        |
        |  FOR tbl IN
        |    SELECT table_name FROM information_schema.tables t
        |    WHERE  table_schema in (sch)
        |      AND  table_name  like 'TD_%'
        |      AND NOT (table_name  like 'TD_%_BIDASK')
        |      AND NOT (table_name  like 'TD_%_LAST')
        |      AND EXISTS (SELECT 1 FROM information_schema.columns c
        |                  WHERE c.table_name = t.table_name
        |                    AND c.table_schema = t.table_schema
        |                    AND c.column_name = 'TYP')
        |  LOOP
        |  EXECUTE format('ALTER TABLE %I.%I DROP COLUMN "TYP"', sch, tbl);
        |  END LOOP;
        |
        |  FOR tbl IN
        |    SELECT table_name FROM information_schema.tables
        |    WHERE  table_schema in (sch)
        |      AND  table_name  like 'TD_%'
        |      AND NOT (table_name  like 'TD_%_BIDASK')
        |      AND NOT (table_name  like 'TD_%_LAST')
        |  LOOP
        |  EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS %I ON %I.%I("TIME")', 'IND_TIME_' || tbl, sch, tbl);
        |  END LOOP;
        |
        |  END $$ LANGUAGE 'plpgsql';
        |  COMMIT;""".stripMargin
    val sqlUpdate = SQLActionBuilder(Seq(updateTable), SetParameter.SetUnit)
    Await.result(DB.db.run(sqlUpdate.as[Unit]), Duration.Inf)
    ConfigRepo.deleteConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "")))
    ConfigRepo.putConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
      Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, "3")))
    DB.createTables("4")
    val checkUpdate =
      """select string_agg(column_name, ',' order by column_name)
        | from information_schema.columns
        | where table_schema = 'public'
        |   and table_name = 'TICKER'""".stripMargin
    val sqlCheckUpdate = SQLActionBuilder(Seq(checkUpdate), SetParameter.SetUnit).as[String]
    val columns: String = Await.result(DB.db.run(sqlCheckUpdate), Duration.Inf).head
    assert(columns == "BAR_SIZE,CURRENCY,EXCHANGE,ID,LAST_TRADE_DATE_OR_CONTRACT_MONTH,LOCAL_SYMBOL,MULTIPLIER,NAME,PREC,PRIMARY_EXCHANGE,RIGHT,STRIKE,TYP")
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
    val ticker = TestData.createTickerTypeDB(0, "TEST", "EXC", "STK", BarSizes.MIN15, 2)
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
    val ticker = TestData.createTickerTypeDB(0, "TEST", "EXC", "STK", BarSizes.MIN15, 2, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty)
    val tickerType: TickerLoadType = ticker
    TickerDataRepo.createTableIfNotExists(tickerType)
    val tickerDataTable = TickerDataTable.getQuery(tickerType)
    val clazz = new TickerDataTableGen(tickerType)
    val query = TableQuery[clazz.TickerDataTable]
    Await.result(DB.db.run(query.filter(td => td.time === 1000L).delete), Duration.Inf)
    val historicalData = HistoricalData(1000, 2000, 1000, 1500, 1600, 1000, HistoricalDataType.TRADES)
    Await.result(DB.db.run(tickerDataTable += historicalData), Duration.Inf)
    val testTickerData = Await.result(DB.db.run(query.filter(td => td.time === 1000L).result.head), Duration.Inf)
    val testHistoricalData: HistoricalData = testTickerData
    Await.result(DB.db.run(query.filter(td => td.time === 1000L).delete), Duration.Inf)
    assert(historicalData == testHistoricalData)
  }

  test("try creating ticker data record for FUTURES") {
    DB.createTables()
    val ticker = TestData.createTickerTypeDB(0, "TEST", "EXC", "FUT", BarSizes.MIN15, 2, Option.empty, Option.empty, Option.empty, Option.empty, Some("202306"), Option.empty)
    val tickerType: TickerLoadType = ticker
    TickerDataRepo.createTableIfNotExists(tickerType)
    val tickerDataTable = TickerDataTable.getQuery(tickerType)
    val clazz = new TickerDataTableGen(tickerType)
    val query = TableQuery[clazz.TickerDataTable]
    Await.result(DB.db.run(query.filter(td => td.time === 1000L).delete), Duration.Inf)
    val historicalData = HistoricalData(1000, 2000, 1000, 1500, 1600, 1000, HistoricalDataType.TRADES)
    Await.result(DB.db.run(tickerDataTable += historicalData), Duration.Inf)
    val testTickerData = Await.result(DB.db.run(query.filter(td => td.time === 1000L).result.head), Duration.Inf)
    val testHistoricalData: HistoricalData = testTickerData
    Await.result(DB.db.run(query.filter(td => td.time === 1000L).delete), Duration.Inf)
    assert(historicalData == testHistoricalData)
  }

  test("try creating exchange records") {
    DB.createTables()
    Await.result(DB.db.run(ExchangeTable.query.filter(_.name === "TEST").delete), Duration.Inf)
    val testExchangeId: Int = ExchangeRepo.getExchangeId("TEST")
    val actualId = Await.result(DB.db.run(ExchangeTable.query.filter(_.name === "TEST").result), Duration.Inf).head.id
    assert(testExchangeId == actualId)
  }

  test("try creating ticker tick records") {
    DB.createTables()
    val ticker = TestData.createTickerTypeDB(0, "TEST", "EXC", "STK", BarSizes.TICK, 2, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty, Option.empty)
    val tickerType: TickerLoadType = ticker
    val expectedLast = Seq(TickerTickLast(0, 10, 10, 1.0, 1.0, 1, "A", pastLimit = true, unreported = false))
    TickerTickRepo.writeLast(tickerType, expectedLast, checkAlreadyInDb = false)
    val expectedBidAsk = Seq(TickerTickBidAsk(0, 10, 10, 1.0, 1.0, 1.0, 1.0, bidPastLow = true, askPastHigh = false))
    TickerTickRepo.writeBidAsk(tickerType, expectedBidAsk, checkAlreadyInDb = false)

    val actualLast = TickerTickRepo.readLast(tickerType, 10, 10).map { l => l.copy(id = 0) }
    val actualBidAsk = TickerTickRepo.readBidAsk(tickerType, 10, 10).map { ba => ba.copy(id = 0) }

    TickerTickRepo.removeLast(tickerType, 10, 10)
    TickerTickRepo.removeBidAsk(tickerType, 10, 10)

    assert(actualLast == expectedLast)
    assert(actualBidAsk == expectedBidAsk)
  }

  test("try creating ticker tick records for FUTURES") {
    DB.createTables()
    val ticker = TestData.createTickerTypeDB(0, "TEST", "EXC", "FUT", BarSizes.TICK, 2, Option.empty, Option.empty, Option.empty, Option.empty, Some("202306"), Option.empty)
    val tickerType: TickerLoadType = ticker
    val expectedLast = Seq(TickerTickLast(0, 10, 10, 1.0, 1.0, 1, "A", pastLimit = true, unreported = false))
    TickerTickRepo.writeLast(tickerType, expectedLast, checkAlreadyInDb = false)
    val expectedBidAsk = Seq(TickerTickBidAsk(0, 10, 10, 1.0, 1.0, 1.0, 1.0, bidPastLow = true, askPastHigh = false))
    TickerTickRepo.writeBidAsk(tickerType, expectedBidAsk, checkAlreadyInDb = false)

    val actualLast = TickerTickRepo.readLast(tickerType, 10, 10).map { l => l.copy(id = 0) }
    val actualBidAsk = TickerTickRepo.readBidAsk(tickerType, 10, 10).map { ba => ba.copy(id = 0) }

    TickerTickRepo.removeLast(tickerType, 10, 10)
    TickerTickRepo.removeBidAsk(tickerType, 10, 10)

    assert(actualLast == expectedLast)
    assert(actualBidAsk == expectedBidAsk)
  }
}

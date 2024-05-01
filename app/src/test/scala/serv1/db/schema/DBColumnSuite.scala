package serv1.db.schema

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.DB
import serv1.db.DB.db
import serv1.db.schema.generic.TableSchemaUpdater
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import slick.jdbc.{SQLActionBuilder, SetParameter}
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class DBColumnSuite extends AnyFunSuite with Logging {
  test("Test column adding") {
    val columnName = "TIME"
    val tableName = EarningsEventTable.query.baseTableRow.tableName
    val updater = new TableSchemaUpdater[EarningsEvent, EarningsEventTable, TableQuery[EarningsEventTable]]()
    val mTable = Await.result(db.run(MTable.getTables), Duration.Inf).find(_.name.name == tableName)
    assert(mTable.nonEmpty)
    removeColumn(tableName, columnName)
    updater.update(EarningsEventTable.query, mTable.get)
    checkColumnExists(tableName, columnName, "integer")
  }

  def removeColumn(tableName: String, columnName: String): Unit = {
    val updateTable: String =
      s"""START TRANSACTION;
         |  ALTER TABLE "$tableName" DROP COLUMN IF EXISTS "$columnName";
         |  COMMIT;""".stripMargin
    val sqlUpdate = SQLActionBuilder(Seq(updateTable), SetParameter.SetUnit)
    Await.result(DB.db.run(sqlUpdate.as[Unit]), Duration.Inf)
  }

  def checkColumnExists(tableName: String, columnName: String, columnType: String): Unit = {
    val checkColumn =
      s"""SELECT data_type FROM information_schema.columns
         |    WHERE  table_schema in ('public')
         |     and column_name  = '$columnName'
         |      AND  table_name  like '$tableName'
         |      """.stripMargin
    val sqlCheckUpdate = SQLActionBuilder(Seq(checkColumn), SetParameter.SetUnit).as[String]
    val columnTypeDB: String = Await.result(DB.db.run(sqlCheckUpdate), Duration.Inf).head
    assert(columnTypeDB == columnType)
  }
}

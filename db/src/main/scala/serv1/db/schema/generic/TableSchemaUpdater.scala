package serv1.db.schema.generic

import serv1.db.DB
import slick.ast.FieldSymbol
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.{MColumn, MIndexInfo, MTable}
import slick.jdbc.{PostgresProfile, SQLActionBuilder, SetParameter}
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TableSchemaUpdater[P, S <: PostgresProfile.Table[P], Q <: TableQuery[S]] extends Logging {

  val nullable = "NULL"
  val notNullable = "NOT NULL"
  val scalaOption = "scala.Option"

  val addColumnTemplate = "ALTER TABLE \"%1$s\" ADD COLUMN \"%2$s\" %3$s %4$s"

  def updateColumn(fieldSymbol: FieldSymbol, columns: Map[String, MColumn], _indexInfo: Vector[MIndexInfo], mTable: MTable): Unit = {
    val jdbcTypeName = PostgresProfile.jdbcTypeFor(fieldSymbol.tpe).sqlTypeName(Some(fieldSymbol))
    val columnName = fieldSymbol.name
    if (!columns.contains(columnName)) {
      val dbSql = addColumnTemplate.format(mTable.name.name, fieldSymbol.name, jdbcTypeName,
        if (fieldSymbol.tpe.classTag.runtimeClass.getCanonicalName == scalaOption) nullable else notNullable)
      logger.info(s"Adding column: $dbSql")
      val sqlUpdate = SQLActionBuilder(Seq(dbSql), SetParameter.SetUnit)
      Await.result(DB.db.run(sqlUpdate.as[Unit]), Duration.Inf)
    }
  }

  def update(tableQuery: Q, mTable: MTable): Unit = {
    val dbColumns = Await.result(DB.db.run(mTable.getColumns), Duration.Inf).map(s => (s.name, s)).toMap
    val indexInfo = Await.result(DB.db.run(mTable.getIndexInfo()), Duration.Inf)
    val columns = tableQuery.baseTableRow.create_*.toSeq
    val indexes = tableQuery.baseTableRow.indexes.toSeq
    columns.foreach(updateColumn(_, dbColumns, indexInfo, mTable))
  }
}

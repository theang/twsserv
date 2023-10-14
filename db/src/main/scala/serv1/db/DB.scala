package serv1.db

import serv1.db.repo.impl.ConfigRepo
import serv1.db.schema._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import slick.jdbc.{SQLActionBuilder, SetParameter}
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object DB extends Logging {
  val DBConfigPath = "postgres"
  val db = Database.forConfig(DBConfigPath)

  def upgradeDatabaseVersionIfNeeded(dbSchemaVersion: String = Configuration.DATABASE_SCHEMA_VERSION): Unit = {
    val configs: Seq[Config] = ConfigRepo.getConfigsByType(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP)
      .filter(_.name == Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME)
    val dbVersion: String = if (configs.isEmpty) {
      "0"
    } else {
      configs.head.value
    }
    logger.info(s"DATABASE_SCHEMA_VERSION in db: $dbVersion, current version in config: $dbSchemaVersion")
    if (dbSchemaVersion < dbVersion) {
      throw new IllegalArgumentException(s"DATABASE_SCHEMA_VERSION in db: $dbVersion is bigger than current version in config: $dbSchemaVersion")
    }
    if (dbSchemaVersion > dbVersion) {
      val convertScript = DBSchemaUpgrade.getSchemaUpgradeCommand(dbVersion, dbSchemaVersion)
      logger.info(s"upgrading db using script: $convertScript")
      val sqlScript = SQLActionBuilder(Seq(convertScript), SetParameter.SetUnit)
      Await.result(DB.db.run(sqlScript.as[Unit]), Duration.Inf)
      ConfigRepo.putConfigs(List(Config(Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_TYP,
        Configuration.DATABASE_SCHEMA_VERSION_PARAMETER_NAME, dbSchemaVersion)))
    } else {
      logger.info("No need for database update")
    }
  }

  def createTables(dbSchemaVersion: String = Configuration.DATABASE_SCHEMA_VERSION): Seq[Unit] = {
    val dbTables = Await.result(db.run(MTable.getTables), Duration.Inf).map(_.name.name)
    val tablesBeforeSchemaUpgrade = List(
      ConfigTable.query
    ).filter(q => !dbTables.contains(q.baseTableRow.tableName))
    val actionBeforeSchemaUpgrade = for (t <- tablesBeforeSchemaUpgrade) yield t.schema.createIfNotExists
    Await.result(db.run(DBIO.sequence(actionBeforeSchemaUpgrade)), Duration.Inf)

    upgradeDatabaseVersionIfNeeded(dbSchemaVersion)

    val tables = List(JobTable.query,
      JobTable.archiveQuery,
      TickerTypeTable.query,
      ScheduledTaskTable.query,
      TickerDataErrorsTable.query,
      TickerTrackingTable.query,
      ExchangeTable.query,
      EventTable.query,
      EarningsEventTable.query
    ).filter(q => !dbTables.contains(q.baseTableRow.tableName))
    val action = for (t <- tables) yield t.schema.createIfNotExists
    Await.result(db.run(DBIO.sequence(action)), Duration.Inf)
  }
}

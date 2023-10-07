package serv1.db

object Configuration {
  val DATABASE_SCHEMA_VERSION_PARAMETER_TYP = "database"
  val DATABASE_SCHEMA_VERSION_PARAMETER_NAME = "DATABASE_SCHEMA_VERSION"
  val DATABASE_SCHEMA_VERSION: String = "3"
  val DATABASE_SCHEMA_UPGRADE_FILE: String = "schema_upgrade.yaml"
  val INITIAL_JOB_REPO_WAIT_LOCK_DURATION = 2

  val defaultPrecision: Int = 2
}

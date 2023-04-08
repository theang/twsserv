package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.schema.{Config, ConfigTable}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ConfigRepo {
  implicit var duration: Duration.Infinite = Duration.Inf

  def getConfigsByType(typ: String): Seq[Config] = {
    Await.result(DB.db.run(ConfigTable.query.filter(_.typ === typ).result), duration)
  }

  def deleteConfigs(configs: Seq[Config]): Int = {
    val tableQuery = ConfigTable.query
    val actions = configs.map(config => tableQuery.filter(tt => tt.typ === config.typ && tt.name === config.name).delete)
    Await.result(DB.db.run(DBIO.sequence(actions)), duration).sum
  }

  def putConfigs(configs: Seq[Config]): Unit = {
    deleteConfigs(configs)
    Await.result(DB.db.run(DBIO.seq(ConfigTable.query ++= configs)), duration)
  }
}

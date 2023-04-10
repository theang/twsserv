package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.repo.intf.ExchangeRepoIntf
import serv1.db.schema.{Exchange, ExchangeTable}
import slick.jdbc.PostgresProfile.api._
import slick.util.Logging

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ExchangeRepo extends ExchangeRepoIntf with Logging {
  var duration: Duration = Duration.Inf

  val map: ConcurrentHashMap[String, Int] = new ConcurrentHashMap[String, Int]()

  override def getExchangeId(name: String): Int = {
    if (map.containsKey(name)) {
      map.get(name)
    } else {
      val result = Await.result(DB.db.run(ExchangeTable.query.filter(_.name === name).map(_.id).result), duration)
      if (result.isEmpty) {
        logger.warn(s"getExchangeId: exchange with name: $name was not found, creating new record")
        val query = ExchangeTable.query
        val result: Int = Await.result(DB.db.run(query returning query.map(_.id) += Exchange(0, name)), duration)
        map.put(name, result)
        result
      } else {
        result.head
      }
    }
  }
}

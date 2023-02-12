package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.repo.intf.TickerTrackingRepoIntf
import serv1.db.schema.{TickerTracking, TickerTrackingTable}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TickerTrackingRepo extends TickerTrackingRepoIntf {
  def addTickersTracking(scheduler: Int, tickerTypes: Seq[Int]): Int = {
    val tableQuery = TickerTrackingTable.query
    val action = tableQuery ++= tickerTypes.map(tickerType => TickerTracking(0, tickerType, scheduler))
    Await.result(DB.db.run(action), Duration.Inf).sum
  }

  def findTickerTracking(scheduler: Int): Seq[Int] = {
    val tableQuery = TickerTrackingTable.query
    val action = tableQuery.filter(_.schedule === scheduler).map(_.tickerType).result
    Await.result(DB.db.run(action), Duration.Inf)
  }

  def removeTickersTracking(scheduler: Int, tickersType: Seq[Int]): Int = {
    val tableQuery = TickerTrackingTable.query
    val actions = tickersType.map(tickerType => tableQuery.filter(tt => tt.schedule === scheduler && tt.tickerType === tickerType).delete)
    Await.result(DB.db.run(DBIO.sequence(actions)), Duration.Inf).sum
  }

  def truncate : Int = {
    Await.result(DB.db.run(TickerTrackingTable.query.delete), Duration.Inf)
  }
}

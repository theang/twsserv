package serv1.db.repo.intf


trait TickerTrackingRepoIntf {
  def addTickersTracking(scheduler: Int, tickersType: Seq[Int]): Int

  def findTickerTracking(scheduler: Int): Seq[Int]

  def removeTickersTracking(scheduler: Int, tickersType: Seq[Int]): Int
}

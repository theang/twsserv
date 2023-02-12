package serv1.db.repo.intf

import serv1.model.ticker.TickerLoadType

trait TickerTypeRepoIntf {
  def addTickerType(tt: TickerLoadType): Unit
  def queryTickers(): Seq[TickerLoadType]
  def queryTickers(ids: Seq[Int]): Seq[TickerLoadType]
  def queryTickerType(tt: TickerLoadType): Int
  def removeTickerType(tt: TickerLoadType): Int

}

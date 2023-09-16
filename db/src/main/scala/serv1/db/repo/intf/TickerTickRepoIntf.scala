package serv1.db.repo.intf

import serv1.db.schema.{TickerTickBidAsk, TickerTickLast}
import serv1.model.ticker.TickerLoadType

trait TickerTickRepoIntf {
  def writeLast(ticker: TickerLoadType, data: Seq[TickerTickLast], checkAlreadyInDb: Boolean): Unit

  def writeBidAsk(ticker: TickerLoadType, data: Seq[TickerTickBidAsk], checkAlreadyInDb: Boolean): Unit

  def readLast(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Seq[TickerTickLast]

  def readBidAsk(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Seq[TickerTickBidAsk]

  def removeLast(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Int

  def removeBidAsk(ticker: TickerLoadType, timeFrom: Long, timeTo: Long): Int
}

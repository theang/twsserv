package serv1.db.repo.intf

import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType

trait TickerDataRepoIntf {
  def write(ticker: TickerLoadType, data: Seq[HistoricalData]): Unit

  def writeUpdate(ticker: TickerLoadType, data: Seq[HistoricalData]): Unit

  def read(ticker: TickerLoadType): Seq[HistoricalData]

  def readRange(ticker: TickerLoadType, from: Long, to: Long): Seq[HistoricalData]

  def truncate(ticker: TickerLoadType): Unit

  def latestDate(ticker: TickerLoadType): Option[Long]

  def earliestDate(ticker: TickerLoadType): Option[Long]
}

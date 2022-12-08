package serv1.job

import serv1.client.DataClient
import serv1.client.converters.BarSizeConverter
import serv1.db.repo.{JobRepo, TickerDataRepo}
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import serv1.util.LocalDateTimeUtil
import slick.util.Logging

import java.time.LocalDateTime
import java.util.UUID


class TickerJobService (client: DataClient) extends Logging {

  def saveHistoricalData(jobId: UUID, ticker: TickerLoadType, data: List[HistoricalData], last: Boolean) = {
    TickerDataRepo.write(ticker, data)
    if (last) {
      JobRepo.updateJob(jobId, ticker)
    }
  }

  def errorCallback(code: Int, msg: String): Unit =
    logger.error(s"Client error: code = ${code}, message = ${msg}")

  def loadTicker(jobId: UUID, ticker: TickerLoadType, from: LocalDateTime, to: LocalDateTime) = {
    client.loadHistoricalData(LocalDateTimeUtil.toEpoch(from),
      LocalDateTimeUtil.toEpoch(to),
      ticker.tickerType.name,
      ticker.tickerType.exchange,
      ticker.tickerType.typ,
      BarSizeConverter.getBarSizeSeconds(ticker.barSize),
      ticker.tickerType.prec,
      (data: List[HistoricalData], last: Boolean) => saveHistoricalData(jobId, ticker, data, last),
      errorCallback)
  }

  def loadTickers(jobId: UUID, tickers: List[TickerLoadType],
                  from: LocalDateTime,
                  to: LocalDateTime) = tickers.foreach {loadTicker(jobId, _, from, to)}
}

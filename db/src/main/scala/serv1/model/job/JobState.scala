package serv1.model.job

import serv1.model.job.JobStatuses.JobStatus
import serv1.model.ticker.{TickerError, TickerLoadType}

import java.time.LocalDateTime

sealed trait JobState

case class TickerJobState(status: JobStatus,
                          tickers: List[TickerLoadType],
                          loadedTickers: List[TickerLoadType],
                          ignoredTickers: List[TickerLoadType],
                          errors: List[TickerError],
                          from: LocalDateTime,
                          to: LocalDateTime,
                          overwrite: Boolean) extends JobState
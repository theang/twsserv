package serv1.job

import akka.http.scaladsl.model.DateTime
import serv1.model.job.JobStatuses.JobStatus
import serv1.model.ticker.{TickerLoadType, TickerType}

import java.time.LocalDateTime

sealed trait JobState
case class TickerJobState(val status:JobStatus,
                          val tickers:List[TickerLoadType],
                          val loadedTickers:List[TickerLoadType],
                          val from:LocalDateTime,
                          val to:LocalDateTime) extends JobState

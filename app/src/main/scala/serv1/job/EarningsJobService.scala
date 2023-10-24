package serv1.job

import com.typesafe.config.Config
import serv1.client.DataClient
import serv1.config.ServConfig
import serv1.db.repo.intf.{EventRepoIntf, JobRepoIntf}
import serv1.model.event.EarningsEventType
import serv1.util.LocalDateTimeUtil

import java.time.LocalDateTime
import java.util.UUID

object EarningsJobService {
  var config: Config = ServConfig.config.getConfig("earningsJob")
  val earningsJobScheduleName: String = config.getString("scheduleName")
}

class EarningsJobService(client: DataClient, jobRepo: JobRepoIntf, eventRepo: EventRepoIntf) {
  def loadEarningsForDay(jobId: UUID, date: LocalDateTime): Unit = {
    val earningDate = LocalDateTimeUtil.toEpoch(date.toLocalDate.atStartOfDay())
    val earnings = client.getEarningsForDate(earningDate)
    eventRepo.insertEarningsEvents(earnings.map { earning => EarningsEventType(earning.symbol, earningDate, "", earning.forecast, earning.fiscalQuarterEnding, earning.eps, earning.epsForecast, earning.marketCap, earning.lastYearEps, earning.lastYearDate) })
  }

  def updateEarningsJob(jobId: UUID, date: LocalDateTime, to: LocalDateTime): Option[LocalDateTime] = {
    val nextDay = date.plusDays(1)
    if (to.compareTo(nextDay) < 0) {
      jobRepo.finishEarningsLoadingJob(jobId)
      None
    } else {
      jobRepo.updateEarningsLoadingJob(jobId, nextDay)
      Some(nextDay)
    }
  }
}

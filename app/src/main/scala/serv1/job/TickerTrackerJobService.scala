package serv1.job

import serv1.db.repo.intf.{ScheduledTaskRepoIntf, TickerDataRepoIntf, TickerTrackingRepoIntf, TickerTypeRepoIntf}
import serv1.model.ticker.BarSizes.{BarSize, DAY, HOUR, MIN15}
import serv1.rest.loaddata.LoadService
import serv1.util.{CronUtil, LocalDateTimeUtil}

class TickerTrackerJobService(loadService: LoadService, scheduledTaskRepoIntf: ScheduledTaskRepoIntf, tickerTypeRepoIntf: TickerTypeRepoIntf, tickerTrackingRepoIntf: TickerTrackingRepoIntf, tickerDataRepoIntf: TickerDataRepoIntf) {
  val DEFAULT_YEARS_FOR_DAILY = 5
  val DEFAULT_MONTHS_FOR_HOURLY = 3
  val DEFAULT_DAYS_FOR_15MIN = 7

  def defaultLoadPeriods(barSize: BarSize, epoch: Long): Long = {
    val localDate = LocalDateTimeUtil.fromEpoch(epoch)
    val result = barSize match {
      case DAY =>
        localDate.minusYears(DEFAULT_YEARS_FOR_DAILY)
      case HOUR =>
        localDate.minusMonths(DEFAULT_MONTHS_FOR_HOURLY)
      case MIN15 =>
        localDate.minusDays(DEFAULT_DAYS_FOR_15MIN)
    }
    LocalDateTimeUtil.toEpoch(result)
  }

  def runCurrentTrackingJobs(currentEpoch: Long): Unit = {
    scheduledTaskRepoIntf.getScheduledTasksBeforeNextRun(currentEpoch).foreach(scheduledTask => {
      val scheduleName = scheduledTask.name
      scheduledTaskRepoIntf.updateNextRun(scheduleName, CronUtil.findNextRun(currentEpoch, scheduledTask.schedule))
      val tickers = tickerTypeRepoIntf.queryTickers(
        tickerTrackingRepoIntf.findTickerTracking(scheduledTask.id))
      tickers.groupBy({ ticker =>
        val from = tickerDataRepoIntf.latestDate(ticker).getOrElse({
          defaultLoadPeriods(ticker.barSize, currentEpoch)
        })
        from
      }).foreach { case (from, tickers) =>
        loadService.load(tickers,
          LocalDateTimeUtil.fromEpoch(from),
          LocalDateTimeUtil.fromEpoch(currentEpoch))
      }
    })
  }
}

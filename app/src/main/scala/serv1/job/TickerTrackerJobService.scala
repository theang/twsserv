package serv1.job

import serv1.Configuration.INITIAL_TICKER_TRACKER_SCHEDULED_TASKS_ENABLED
import serv1.client.converters.BarSizeConverter
import serv1.db.repo.intf.{ScheduledTaskRepoIntf, TickerDataRepoIntf, TickerTrackingRepoIntf, TickerTypeRepoIntf}
import serv1.model.ticker.BarSizes.{BarSize, DAY, HOUR, MIN15}
import serv1.rest.loaddata.LoadService
import serv1.util.{CronUtil, LocalDateTimeUtil}
import slick.util.Logging

class TickerTrackerJobService(loadService: LoadService,
                              scheduledTaskRepoIntf: ScheduledTaskRepoIntf,
                              tickerTypeRepoIntf: TickerTypeRepoIntf,
                              tickerTrackingRepoIntf: TickerTrackingRepoIntf,
                              tickerDataRepoIntf: TickerDataRepoIntf)
  extends Logging {
  val DEFAULT_YEARS_FOR_DAILY = 5
  val DEFAULT_MONTHS_FOR_HOURLY = 3
  val DEFAULT_DAYS_FOR_15MIN = 7

  var tickerTrackerScheduleEnabled: Boolean = INITIAL_TICKER_TRACKER_SCHEDULED_TASKS_ENABLED

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

  def roundEpochByBar(barSize: BarSize, epoch: Long): Long = {
    val barSizeSeconds = BarSizeConverter.getBarSizeSeconds(barSize)
    val reminder = epoch % barSizeSeconds
    epoch - reminder
  }

  def runTrackingJob(currentEpoch: Long, taskName: String, taskId: Int): Unit = {
    val tickers = tickerTypeRepoIntf.queryTickers(
      tickerTrackingRepoIntf.findTickerTracking(taskId))
    tickers.groupBy({ ticker =>
      val from = tickerDataRepoIntf.latestDate(ticker).getOrElse({
        defaultLoadPeriods(ticker.barSize, currentEpoch)
      })
      roundEpochByBar(ticker.barSize, from) - BarSizeConverter.getBarSizeSeconds(ticker.barSize)
    }).foreach { case (from, tickers) =>
      logger.info(s"$taskName : running $tickers from $from")
      loadService.load(tickers,
        LocalDateTimeUtil.fromEpoch(from),
        LocalDateTimeUtil.fromEpoch(currentEpoch),
        overwrite = true)
    }
  }

  def runCurrentTrackingJobs(currentEpoch: Long): Unit = {
    scheduledTaskRepoIntf.getScheduledTasksBeforeNextRun(currentEpoch).foreach(scheduledTask => {
      val scheduleName = scheduledTask.name
      if (tickerTrackerScheduleEnabled) {
        scheduledTaskRepoIntf.updateNextRun(scheduleName, CronUtil.findNextRun(currentEpoch, scheduledTask.schedule))
        runTrackingJob(currentEpoch, scheduleName, scheduledTask.id)
      } else {
        logger.info(s"serv1.job.TickerTrackerJobService.tickerTrackerScheduleEnabled is disabled, job $scheduleName is not run, and next run: ${LocalDateTimeUtil.fromEpoch(scheduledTask.nextRun)} is not updated")
      }
    })
  }
}

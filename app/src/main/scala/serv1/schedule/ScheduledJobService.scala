package serv1.schedule

import serv1.Configuration.INITIAL_TICKER_TRACKER_SCHEDULED_TASKS_ENABLED
import serv1.db.repo.intf.ScheduledTaskRepoIntf
import serv1.job.{EarningsJobService, TickerTrackerJobService}
import serv1.rest.services.loaddata.earnings.EarningsLoadService
import serv1.util.{CronUtil, LocalDateTimeUtil}
import slick.util.Logging

class ScheduledJobService(tickerTrackerJobService: TickerTrackerJobService,
                          earningsLoadService: EarningsLoadService,
                          scheduledTaskRepoIntf: ScheduledTaskRepoIntf) extends Logging {
  var tickerTrackerScheduleEnabled: Boolean = INITIAL_TICKER_TRACKER_SCHEDULED_TASKS_ENABLED

  def runJob(currentEpoch: Long, scheduleName: String, scheduleId: Int): Unit = {
    scheduleName match {
      case EarningsJobService.earningsJobScheduleName =>
        earningsLoadService.scheduledEarningsJob(currentEpoch)
      case _ =>
        tickerTrackerJobService.runTrackingJob(currentEpoch, scheduleName, scheduleId)
    }
  }

  def runCurrentScheduledJobs(currentEpoch: Long): Unit = {
    scheduledTaskRepoIntf.getScheduledTasksBeforeNextRun(currentEpoch).foreach(scheduledTask => {
      val scheduleName = scheduledTask.name
      if (tickerTrackerScheduleEnabled) {
        scheduledTaskRepoIntf.updateNextRun(scheduleName, CronUtil.findNextRun(currentEpoch, scheduledTask.schedule))
        runJob(currentEpoch, scheduledTask.name, scheduledTask.id)
      } else {
        logger.info(s"serv1.job.ScheduledJobService.runCurrentScheduledJobs is disabled, job $scheduleName is not run, and next run: ${LocalDateTimeUtil.fromEpoch(scheduledTask.nextRun)} is not updated")
      }
    })
  }
}

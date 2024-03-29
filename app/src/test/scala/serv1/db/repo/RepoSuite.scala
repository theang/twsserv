package serv1.db.repo

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.DB
import serv1.db.TestData._
import serv1.db.repo.impl._
import serv1.db.schema.{ExchangeTable, JobTable, ScheduledTask, TickerDataErrors}
import serv1.model.job.{JobStatuses, TickLoadingJobState}
import serv1.model.ticker.TickerError
import serv1.util.LocalDateTimeUtil
import slick.jdbc.PostgresProfile.api._
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class RepoSuite extends AnyFunSuite with Logging {
  test("JobRepo test, create job, update job, check job is complete") {
    assert(JobRepo.getTickerJobStates(null).size === 0)
    val id = JobRepo.createTickerJob(testTickers, from, to, overwrite = false)
    assert(JobRepo.getTickerJobStates(id).size === 1)
    (() => {
      val job = JobRepo.getTickerJobStates(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === true
        job.tickers === testTickers
        job.overwrite === false
      }
    })()
    JobRepo.updateJob(id, testTicker)
    (() => {
      val job = JobRepo.getTickerJobStates(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === List(testTicker)
        job.tickers === List(testTicker2)
        job.overwrite === false
      }
    })()
    JobRepo.updateJob(id, testTicker2)
    (() => {
      val job = JobRepo.getTickerJobStates(id).head
      assert {
        job.status === JobStatuses.FINISHED
        job.loadedTickers.isEmpty === testTickers
        job.tickers.isEmpty === true
        job.overwrite === false
      }
    })()
    JobRepo.removeJob(id)
  }

  test("JobRepo TickLoading test, create job, update job, check job is complete") {
    JobRepo.removeJob(null)
    assert(JobRepo.getTickerJobs[TickLoadingJobState](null).size === 0)
    val id = JobRepo.createTickLoadingJob(testTickers)
    assert(JobRepo.getTickerJobs[TickLoadingJobState](id).size === 1)
    (() => {
      val (_, job) = JobRepo.getTickerJobs[TickLoadingJobState](id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.tickers === testTickers
      }
    })()
    (() => {
      val ids = JobRepo.findTickLoadingJobByLoadType(testTickers.head)
      val (_, job) = JobRepo.getTickerJobs[TickLoadingJobState](ids.head).head
      assert {
        ids.size === 1
        ids.head === id
        job.status === JobStatuses.IN_PROGRESS
        job.tickers === testTickers
      }
    })()
    JobRepo.cancelTickLoadingJob(id)
    (() => {
      val (_, job) = JobRepo.getTickerJobs[TickLoadingJobState](id).head
      assert {
        job.status === JobStatuses.FINISHED
        job.tickers === testTickers
      }
    })()
    JobRepo.removeJob(id)
  }

  test("JobRepo error test, create job, update with error, test result") {
    assert(JobRepo.getTickerJobStates(null).size === 0)
    val id = JobRepo.createTickerJob(testTickers, from, to, overwrite = false)
    JobRepo.updateJob(id, testTicker2, Some("test"), ignore = false)
    (() => {
      val job = JobRepo.getTickerJobStates(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === true
        job.errors === List(TickerError(testTicker2, "test"))
        job.tickers === List(testTicker)
        job.overwrite === false
      }
    })()
    JobRepo.removeJob(id)
  }

  test("JobRepo archive test, create job, archive job") {
    assert(JobRepo.getTickerJobStates(null).size === 0)
    val idTickerJob = JobRepo.createTickerJob(testTickers, from, to, overwrite = false)
    JobRepo.updateJob(idTickerJob, testTicker)
    JobRepo.updateJob(idTickerJob, testTicker2)
    val idTickLoadingJob = JobRepo.createTickLoadingJob(testTickers)
    JobRepo.cancelTickLoadingJob(idTickLoadingJob)
    JobRepo.archiveCompletedJobs()
    (() => {
      val tickerJobs = JobRepo.getTickerJobStates(idTickerJob)
      assert(tickerJobs.isEmpty === true)
      val tickJobs = JobRepo.getTickerJobStates(idTickerJob)
      assert(tickJobs.isEmpty === true)
    })()
    Await.result(DB.db.run(JobTable.archiveQuery.delete), Duration.Inf)
  }

  test("Ticker type repo") {
    TickerTypeRepo.truncate()
    TickerTypeRepo.addTickerType(testTicker)
    val tickers = TickerTypeRepo.queryTickers()
    assert(tickers === List(testTicker))
    TickerTypeRepo.removeTickerType(testTicker)
    val tickersEmpty = TickerTypeRepo.queryTickers()
    assert(tickersEmpty === List())
  }

  test("Ticker data repo tests") {
    TickerDataRepo.truncate(testTicker)
    TickerDataRepo.write(testTicker, List(testHistoricalData))
    TickerDataRepo.write(testTicker, List(testHistoricalData1))
    TickerDataRepo.write(testTicker, List(testHistoricalData2))
    val allVals = TickerDataRepo.read(testTicker)
    assert(allVals === List(testHistoricalData, testHistoricalData1, testHistoricalData2))
    val oneVal = TickerDataRepo.readRange(testTicker, LocalDateTimeUtil.toEpoch(from), LocalDateTimeUtil.toEpoch(from)).toList
    assert(oneVal === List(testHistoricalData))
    val lastVal = TickerDataRepo.readRange(testTicker, LocalDateTimeUtil.toEpoch(to), LocalDateTimeUtil.toEpoch(to)).toList
    assert(lastVal === List(testHistoricalData2))
    TickerDataRepo.truncate(testTicker)
    TickerTypeRepo.removeTickerType(testTicker)
  }

  test("Ticker data error repo tests") {
    DB.createTables()
    TickerDataErrorRepo.truncate
    val testMessage = "test"
    val newId = TickerDataErrorRepo.addError(testMessage)
    val messages = TickerDataErrorRepo.queryMessages.toList
    assert(messages === List(TickerDataErrors(newId, testMessage)))
    val message = TickerDataErrorRepo.queryMessage(newId)
    assert(message === TickerDataErrors(newId, testMessage))
    TickerDataErrorRepo.deleteMessage(newId)
    val noMessages = TickerDataErrorRepo.queryMessages.toList
    assert(noMessages.isEmpty)
  }

  test("Scheduled Tasks Repo tests") {
    DB.createTables()
    ScheduledTaskRepo.truncate
    ScheduledTaskRepo.addScheduledTask(testScheduleName, testSchedule, testScheduleRun)

    var tasks = ScheduledTaskRepo.getScheduledTasksBeforeNextRun(testScheduleRun)
    assert(tasks.size === 1)

    def assertTask(task: ScheduledTask, nextRun: Long, schedule: String, name: String) = {
      assert(task.nextRun === nextRun)
      assert(task.schedule === schedule)
      assert(task.name === name)
    }

    assertTask(tasks.head, testScheduleRun, testSchedule, testScheduleName)

    tasks = ScheduledTaskRepo.getScheduledTaskByName(testScheduleName)
    assert(tasks.size === 1)
    assertTask(tasks.head, testScheduleRun, testSchedule, testScheduleName)

    val id = tasks.head.id
    tasks = ScheduledTaskRepo.getScheduledTaskById(id)
    assert(tasks.size === 1)
    assertTask(tasks.head, testScheduleRun, testSchedule, testScheduleName)

    ScheduledTaskRepo.updateName(testScheduleName, testScheduleName1)
    ScheduledTaskRepo.updateNextRun(testScheduleName1, testScheduleRun1)
    ScheduledTaskRepo.updateSchedule(testScheduleName1, testSchedule1)
    tasks = ScheduledTaskRepo.getScheduledTaskByName(testScheduleName1)

    assert(tasks.size === 1)
    assertTask(tasks.head, testScheduleRun1, testSchedule1, testScheduleName1)

    tasks = ScheduledTaskRepo.getScheduledTasksBeforeNextRun(testScheduleRun)
    assert(tasks.size === 0)

    tasks = ScheduledTaskRepo.getScheduledTasksBeforeNextRun(testScheduleRun1)
    assert(tasks.size === 1)
    assertTask(tasks.head, testScheduleRun1, testSchedule1, testScheduleName1)

    ScheduledTaskRepo.deleteScheduledTask(testScheduleName1)
    tasks = ScheduledTaskRepo.getScheduledTaskByName(testScheduleName1)
    assert(tasks.size === 0)
  }

  test("Exchange repo tests") {
    Await.result(DB.db.run(ExchangeTable.query.delete), Duration.Inf)

    val idTest = ExchangeRepo.getExchangeId("TEST")
    val idTest2 = ExchangeRepo.getExchangeId("TEST")

    assert(idTest === idTest2)

    val idATest = ExchangeRepo.getExchangeId("ATEST")
    val idATest2 = ExchangeRepo.getExchangeId("ATEST")

    assert(idATest === idATest2)

    Await.result(DB.db.run(ExchangeTable.query.delete), Duration.Inf)
    ExchangeRepo.map.clear()

    val id2Test = ExchangeRepo.getExchangeId("TEST")
    val id2Test2 = ExchangeRepo.getExchangeId("TEST")

    assert(idTest !== id2Test)
    assert(id2Test === id2Test2)
  }
}

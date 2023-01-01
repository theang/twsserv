package serv1.db.repo

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.TestData._
import serv1.db.repo.impl.{JobRepo, TickerDataRepo, TickerTypeRepo}
import serv1.model.job.JobStatuses
import serv1.model.ticker.TickerError
import serv1.util.LocalDateTimeUtil

@RunWith(classOf[JUnitRunner])
class RepoSuite extends AnyFunSuite {
  test("JobRepo test, create job, update job, check job is complete") {
    assert(JobRepo.getTickerJobs(null).size === 0)
    val id = JobRepo.createTickerJob(testTickers, from, to)
    assert(JobRepo.getTickerJobs(id).size === 1)
    (() => {
      val job = JobRepo.getTickerJobs(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === true
        job.tickers === testTickers
      }
    })()
    JobRepo.updateJob(id, testTicker)
    (() => {
      val job = JobRepo.getTickerJobs(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === List(testTicker)
        job.tickers === List(testTicker2)
      }
    })()
    JobRepo.updateJob(id, testTicker2)
    (() => {
      val job = JobRepo.getTickerJobs(id).head
      assert {
        job.status === JobStatuses.FINISHED
        job.loadedTickers.isEmpty === testTickers
        job.tickers.isEmpty === true
      }
    })()
    JobRepo.removeJob(id)
  }

  test("JobRepo error test, create job, update with error, test result") {
    assert(JobRepo.getTickerJobs(null).size === 0)
    val id = JobRepo.createTickerJob(testTickers, from, to)
    JobRepo.updateJob(id, testTicker2, Some("test"))
    (() => {
      val job = JobRepo.getTickerJobs(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === true
        job.errors === List(TickerError(testTicker2, "test"))
        job.tickers === List(testTicker)
      }
    })()
    JobRepo.removeJob(id)
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
}

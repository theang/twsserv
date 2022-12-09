package serv1.db.repo

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.TestData._
import serv1.model.job.JobStatuses

@RunWith(classOf[JUnitRunner])
class RepoSuite extends AnyFunSuite {
  test("JobRepo tests") {
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
}

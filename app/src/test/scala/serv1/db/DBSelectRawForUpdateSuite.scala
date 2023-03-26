package serv1.db

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import serv1.db.TestData._
import serv1.db.repo.impl.JobRepo
import serv1.db.schema.JobTable
import serv1.model.job.JobStatuses
import serv1.model.ticker.TickerError
import serv1.rest.JsonFormats
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.util.Logging

import java.util.concurrent.CountDownLatch
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.language.implicitConversions


@RunWith(classOf[JUnitRunner])
class DBSelectRawForUpdateSuite extends AnyFunSuite with JsonFormats with Logging {
  test("Write lock on row should be successfully execute when no lock is acquired") {
    JobRepo.removeJob(null)
    assert(JobRepo.getTickerJobStates(null).size === 0)
    val id = JobRepo.createTickerJob(testTickers, from, to, overwrite = false)
    val result = JobRepo.updateJob(id, testTicker2, Some("test"), ignore = false)
    assert(result)
    (() => {
      val job = JobRepo.getTickerJobStates(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === true
        job.errors === List(TickerError(testTicker2, "test"))
        job.tickers === testTickers
        job.overwrite === false
      }
    })()
    JobRepo.removeJob(id)
  }

  test("Write lock on row should not be successfully execute when a lock is acquired") {
    JobRepo.removeJob(null)
    assert(JobRepo.getTickerJobStates(null).size === 0)
    val id = JobRepo.createTickerJob(testTickers, from, to, overwrite = false)
    val latch = new CountDownLatch(1)
    val latch2 = new CountDownLatch(1)
    val locking = new Thread() {
      override def run(): Unit = {
        val query = JobTable.query.filter(_.jobId === id)
        implicit val db: PostgresProfile.backend.Database = DB.db
        Await.result(new DBSelectRawForUpdate(query).apply { _ =>
          latch.countDown()
          latch2.await()
          DBIO.successful()
        }, Duration.Inf)
      }
    }
    locking.start()
    latch.await()
    val result = JobRepo.updateJob(id, testTicker2, Some("test"), ignore = false)
    latch2.countDown()
    assert(!result)
    (() => {
      val job = JobRepo.getTickerJobStates(id).head
      assert {
        job.status === JobStatuses.IN_PROGRESS
        job.loadedTickers.isEmpty === true
        job.errors.isEmpty === true
        job.tickers === testTickers
      }
    })()
    JobRepo.removeJob(id)
  }
}

package serv1.job

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import serv1.TestService
import serv1.client.DataClient
import serv1.db.TestData._
import serv1.db.repo.intf.{JobRepoIntf, TickerDataRepoIntf}
import serv1.job.TickerJobActor.Run

@RunWith(classOf[JUnitRunner])
class TickerJobSuite extends TestService with MockFactory {

  test("call service") {
    val clientMock = mock[DataClient]
    (clientMock.loadHistoricalData _).expects(*, *, *, *, *, *, *, *, *).returning(()).twice()
    val jobRepo = mock[JobRepoIntf]
    (jobRepo.getTickerJobs _).expects(TestID).returning(List(testTickerJobState))
    val tickerDataRepo = mock[TickerDataRepoIntf]
    val tickerJobService = new TickerJobService(clientMock, jobRepo, tickerDataRepo)
    val tickerJob = testKit.spawn(TickerJobActor(tickerJobService, jobRepo), "tickerJob")
    val probe = testKit.createTestProbe[TickerJobActor.RunSuccessful]()
    tickerJob ! Run(TestID, probe.ref)
    probe.expectMessage(TickerJobActor.RunSuccessful())
    testKit.stop(tickerJob)
  }

}

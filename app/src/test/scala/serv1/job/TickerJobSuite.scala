package serv1.job

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import serv1.TestService
import serv1.client.DataClient
import serv1.db.TestData._
import serv1.db.TickerDataActor
import serv1.db.repo.intf.{ExchangeRepoIntf, JobRepoIntf, TickerDataRepoIntf, TickerTickRepoIntf}
import serv1.job.TickerJobActor.{CheckState, Finished, Run}

@RunWith(classOf[JUnitRunner])
class TickerJobSuite extends TestService with MockFactory {

  test("call service to run Ticker Data loading Job") {
    val clientMock = mock[DataClient]
    (clientMock.loadHistoricalData _).expects(*, *, *, *, *, *, *).returning(()).twice()
    val jobRepo = mock[JobRepoIntf]
    (jobRepo.getJobStates _).expects(TestID).returning(List((TestID, testTickerJobState))).repeat(1)
    (jobRepo.getJobStates _).expects(TestID).returning(List((TestID, testTickerJobStateFinished))).repeat(1)
    (jobRepo.getTickerJobStates _).expects(TestID).returning(List(testTickerJobState)).repeat(2)
    val tickerDataRepo = mock[TickerDataRepoIntf]
    val tickerTickRepo = mock[TickerTickRepoIntf]
    val tickerDataActor = testKit.spawn(TickerDataActor(tickerDataRepo, tickerTickRepo))
    val exchangeRepo = mock[ExchangeRepoIntf]
    val tickerJobService = new TickerJobService(clientMock, jobRepo, tickerDataActor, exchangeRepo)
    val tickerJob = testKit.spawn(TickerJobActor(tickerJobService, jobRepo), "tickerJob")
    val probe = testKit.createTestProbe[TickerJobActor.JobActorResponse]()

    // run job
    tickerJob ! Run(TestID, probe.ref)
    probe.expectMessage(TickerJobActor.RunSuccessful())

    // check job is running
    tickerJob ! CheckState(Set(TestID), probe.ref)
    probe.expectMessage(TickerJobActor.RunningState(Map(TestID -> true)))

    // finish job (remove record from actor cache)
    tickerJob ! Finished(TestID)

    // check job is not running
    tickerJob ! CheckState(Set(TestID), probe.ref)
    probe.expectMessage(TickerJobActor.RunningState(Map(TestID -> false)))

    testKit.stop(tickerJob)
  }

  test("call service to run Ticker Tick Loading Job") {
    val clientMock = mock[DataClient]
    (clientMock.startLoadingTickData _).expects(*, *, *, *).returning((1, 1)).once()
    val jobRepo = mock[JobRepoIntf]
    (jobRepo.getJobStates _).expects(TestID).returning(List((TestID, testTickerTickLoadingJobState)))
    val tickerDataRepo = mock[TickerDataRepoIntf]
    val tickerTickRepo = mock[TickerTickRepoIntf]
    val tickerDataActor = testKit.spawn(TickerDataActor(tickerDataRepo, tickerTickRepo))
    val exchangeRepo = mock[ExchangeRepoIntf]
    val tickerJobService = new TickerJobService(clientMock, jobRepo, tickerDataActor, exchangeRepo)
    val tickerJob = testKit.spawn(TickerJobActor(tickerJobService, jobRepo), "tickerJob")
    val probe = testKit.createTestProbe[TickerJobActor.JobActorResponse]()
    tickerJob ! Run(TestID, probe.ref)
    probe.expectMessage(TickerJobActor.RunSuccessful())
    testKit.stop(tickerJob)
  }
}

package serv1.rest

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector, PostStop}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.RouteConcatenation
import serv1.Configuration
import serv1.client.{MultiClient, PurgeActor}
import serv1.db.repo.impl._
import serv1.db.{ArchiveFinishedJobsActor, TickerDataActor}
import serv1.job._
import serv1.rest.actors.earnings.EarningsLoadActor
import serv1.rest.actors.historical.HistoricalDataActor
import serv1.rest.actors.loaddata
import serv1.rest.actors.schedule.ScheduleActor
import serv1.rest.actors.ticker.TickerJobControlActor
import serv1.rest.controllers.earnings.EarningsLoadRest
import serv1.rest.controllers.loaddata.LoadDataRest
import serv1.rest.controllers.schedule.ScheduleRest
import serv1.rest.controllers.tickdata.TickLoadingRest
import serv1.rest.controllers.ticker.TickerJobControlRest
import serv1.rest.services.loaddata.LoadService
import serv1.rest.services.loaddata.earnings.EarningsLoadService
import serv1.schedule.{ScheduledJobActor, ScheduledJobService}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object RestServer extends RouteConcatenation {
  val databaseDispatcherSelector: DispatcherSelector = DispatcherSelector.fromConfig(Configuration.DATABASE_BLOCKING_DISPATCHER_NAME)

  sealed trait Message

  private final case class StartFailed(cause: Throwable) extends Message

  private final case class Started(binding: ServerBinding) extends Message

  case object Stop extends Message

  def apply(host: String, port: Int): Behavior[Message] = Behaviors.setup { ctx =>

    implicit val system: ActorSystem[Nothing] = ctx.system
    val tickerDataActor = ctx.spawn(TickerDataActor(TickerDataRepo, TickerTickRepo), name = "tickerDataActor", databaseDispatcherSelector)
    val tickerJobService = new TickerJobService(MultiClient, JobRepo, tickerDataActor, ExchangeRepo)
    val tickerActorRef = ctx.spawn(TickerJobActor(tickerJobService, JobRepo), "tickerJobActor")
    val loadService = new LoadService(TickerDataRepo, tickerActorRef)
    val loadDataActorRef = ctx.spawn(loaddata.LoadDataActor(loadService), "loadDataActor")
    val checkJobActorRef = ctx.spawn(loaddata.CheckLoadJobStateActor(loadService), "checkJobStateActor")
    val tickerJobControlActorRef = ctx.spawn(TickerJobControlActor(ScheduledTaskRepo, TickerTrackingRepo, TickerTypeRepo), "tickerJobControlActor")
    val tickerTrackerJobService = new TickerTrackerJobService(loadService,
      ScheduledTaskRepo, TickerTypeRepo, TickerTrackingRepo, TickerDataRepo)
    val earningsJobService = new EarningsJobService(MultiClient, JobRepo, EventRepo)
    val earningsJobActor = ctx.spawn(EarningsJobActor(JobRepo, earningsJobService), "earningsJobActor")
    val earningsLoadService = new EarningsLoadService(earningsJobActor, JobRepo)
    val earningsLoadActor = ctx.spawn(EarningsLoadActor(earningsLoadService), "earningsLoadActor")
    val scheduledJobService = new ScheduledJobService(tickerTrackerJobService, earningsLoadService, ScheduledTaskRepo)
    val scheduledJobActorRef = ctx.spawn(ScheduledJobActor(scheduledJobService), "scheduledJobActor")
    val scheduleActorRef = ctx.spawn(ScheduleActor(ScheduledTaskRepo, tickerTrackerJobService), "scheduleActor")
    val historicalDataActorRef = ctx.spawn(HistoricalDataActor(TickerDataRepo), "historicalDataActor")
    val restartLoadActor = ctx.spawn(RestartLoadActor(JobRepo, tickerActorRef, earningsJobActor), "restartLoadActor")
    val archiveFinishedJobsActor = ctx.spawn(ArchiveFinishedJobsActor(JobRepo), name = "archiveFinishedJobsActor")
    val purgeDataActor = ctx.spawn(PurgeActor(), name = "purgeDataActor")
    val routes = {
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "UP"))
        }
      }
    } ~
      new LoadDataRest(loadDataActorRef, checkJobActorRef, historicalDataActorRef).routes ~
      new ScheduleRest(scheduleActorRef).routes ~
      new TickerJobControlRest(tickerJobControlActorRef).routes ~
      new TickLoadingRest(loadDataActorRef).routes ~
      new EarningsLoadRest(earningsLoadService).routes

    val serverBinding: Future[Http.ServerBinding] =
      Http().newServerAt(host, port).bind(routes)
    ctx.pipeToSelf(serverBinding) {
      case Success(binding) => Started(binding)
      case Failure(ex) => StartFailed(ex)
    }

    def running(binding: ServerBinding): Behavior[Message] =
      Behaviors.receiveMessagePartial[Message] {
        case Stop =>
          ctx.log.info(
            "Stopping server http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          Behaviors.stopped
      }.receiveSignal {
        case (_, PostStop) =>
          binding.unbind()
          Behaviors.same
      }

    def starting(wasStopped: Boolean): Behaviors.Receive[Message] =
      Behaviors.receiveMessage[Message] {
        case StartFailed(cause) =>
          throw new RuntimeException("Server failed to start", cause)
        case Started(binding) =>
          ctx.log.info(
            "Server online at http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          if (wasStopped) ctx.self ! Stop
          running(binding)
        case Stop =>
          // we got a stop message but haven't completed starting yet,
          // we cannot stop until starting has completed
          starting(wasStopped = true)
      }

    starting(wasStopped = false)
  }
}

package serv1.rest

import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.RouteConcatenation
import serv1.client.{MultiClient, TWSClient}
import serv1.db.repo.impl.{JobRepo, TickerDataRepo}
import serv1.job.{TickerJobActor, TickerJobService}
import serv1.rest.loaddata.{CheckLoadJobStateActor, LoadData, LoadDataActor, LoadService}

import scala.util.{Failure, Success}
import scala.concurrent.Future

object RestServer extends RouteConcatenation {
  sealed trait Message

  private final case class StartFailed(cause: Throwable) extends Message

  private final case class Started(binding: ServerBinding) extends Message

  case object Stop extends Message

  def apply(host: String, port: Int): Behavior[Message] = Behaviors.setup { ctx =>

    implicit val system: ActorSystem[Nothing] = ctx.system

    val tickerJobService = new TickerJobService(MultiClient, JobRepo, TickerDataRepo)
    val tickerActorRef = ctx.spawn(TickerJobActor(tickerJobService, JobRepo), "tickerJobActor")
    val loadService = new LoadService(tickerActorRef)
    val loadDataActorRef = ctx.spawn(LoadDataActor(loadService), "loadDataActor")
    val checkJobActorRef = ctx.spawn(CheckLoadJobStateActor(loadService), "checkJobStateActor")
    val routes = {
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "UP"))
        }
      }
    } ~ new LoadData(loadDataActorRef, checkJobActorRef).routes

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

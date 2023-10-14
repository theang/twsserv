package serv1.rest.actors.earnings

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.rest.services.loaddata.earnings.EarningsLoadService

import java.time.LocalDateTime
import java.util.UUID

object EarningsLoadActor {

  sealed trait EarningsLoadActorMessage

  case class EarningsLoadRequest(from: LocalDateTime, to: LocalDateTime)

  case class EarningsLoadRequestMessage(request: EarningsLoadRequest, replyTo: ActorRef[EarningsLoadActorResponse]) extends EarningsLoadActorMessage

  case class StopEarningsLoadRequest(jobId: UUID)

  case class StopEarningsLoadRequestMessage(request: StopEarningsLoadRequest, replyTo: ActorRef[EarningsLoadActorResponse]) extends EarningsLoadActorMessage

  sealed trait EarningsLoadActorResponse

  case class EarningsLoadResponse(jobId: UUID) extends EarningsLoadActorResponse

  case class StopEarningsLoadResponse(success: Boolean) extends EarningsLoadActorResponse

  def apply(earningsLoadService: EarningsLoadService): Behavior[EarningsLoadActorMessage] = {
    Behaviors.receiveMessage[EarningsLoadActorMessage] {
      case EarningsLoadRequestMessage(EarningsLoadRequest(from, to), replyTo) =>
        val jobId = earningsLoadService.createEarningsJob(from, to)
        replyTo ! EarningsLoadResponse(jobId)
        Behaviors.same
      case StopEarningsLoadRequestMessage(StopEarningsLoadRequest(jobId), replyTo) =>
        earningsLoadService.stopEarningsJob(jobId)
        replyTo ! StopEarningsLoadResponse(true)
        Behaviors.same
    }
  }
}

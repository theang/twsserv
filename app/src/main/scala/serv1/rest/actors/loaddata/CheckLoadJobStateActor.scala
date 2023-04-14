package serv1.rest.actors.loaddata

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.model.job.TickerJobState
import serv1.rest.services.loaddata.LoadService

import java.util.UUID

object CheckLoadJobStateActor {
  case class CheckLoadJobRef(jobId: UUID, replyTo: ActorRef[List[TickerJobState]])

  def apply(loadService: LoadService): Behavior[CheckLoadJobRef] = {
    Behaviors.receive { (_, data) =>
      val id = data.jobId
      data.replyTo ! loadService.checkJobStates(id)
      Behaviors.same
    }
  }
}

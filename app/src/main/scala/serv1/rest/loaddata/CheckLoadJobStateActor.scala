package serv1.rest.loaddata

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import serv1.job.TickerJobState
import serv1.rest.loaddata.LoadDataActor.{LoadDataRequest, LoadDataResponse}

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

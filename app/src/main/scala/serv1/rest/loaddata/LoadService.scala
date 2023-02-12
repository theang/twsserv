package serv1.rest.loaddata

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import serv1.db.repo.impl.JobRepo
import serv1.job.{TickerJobActor, TickerJobState}
import serv1.model.ticker.TickerLoadType
import serv1.rest.JsonFormats

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration._

class LoadService(tickerJobActorRef: ActorRef[TickerJobActor.Run])(implicit system: ActorSystem[_]) extends JsonFormats {
  implicit val timeout: Timeout = 1000.seconds

  def load(ticker: List[TickerLoadType], from: LocalDateTime, to: LocalDateTime): UUID = {
    val jobId = JobRepo.createTickerJob(ticker, from, to)
    tickerJobActorRef.ask(replyTo => TickerJobActor.Run(jobId, replyTo))
    jobId
  }

  def checkJobStates(id: UUID): List[TickerJobState] = {
    JobRepo.getTickerJobs(id).toList
  }
}

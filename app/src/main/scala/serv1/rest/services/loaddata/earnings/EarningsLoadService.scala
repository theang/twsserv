package serv1.rest.services.loaddata.earnings

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.typesafe.config.Config
import serv1.Configuration
import serv1.config.ServConfig
import serv1.db.repo.intf.JobRepoIntf
import serv1.job.EarningsJobActor.{EarningsJobActorMessage, StartEarningsLoading, StopEarningsLoading}
import serv1.rest.services.loaddata.earnings.EarningsLoadService.{daysAfter, daysBefore}
import serv1.util.LocalDateTimeUtil

import java.time.LocalDateTime
import java.util.UUID

object EarningsLoadService {
  var config: Config = ServConfig.config.getConfig("earningsJob")
  val daysBefore: Int = config.getInt("daysBefore")
  val daysAfter: Int = config.getInt("daysAfter")
}

class EarningsLoadService(earningsJobActor: ActorRef[EarningsJobActorMessage], jobRepo: JobRepoIntf)(implicit system: ActorSystem[_]) {
  implicit val timeout: Timeout = Configuration.timeout

  def createEarningsJob(from: LocalDateTime, to: LocalDateTime): UUID = {
    val jobId = jobRepo.createEarningsJob(from, to)
    earningsJobActor.ask(replyTo => StartEarningsLoading(jobId, replyTo))
    jobId
  }

  def stopEarningsJob(jobId: UUID): Boolean = {
    earningsJobActor.ask(replyTo => StopEarningsLoading(jobId, replyTo))
    true
  }

  def scheduledEarningsJob(currentEpoch: Long): Unit = {
    val currentDateTime = LocalDateTimeUtil.fromEpoch(currentEpoch)
    createEarningsJob(currentDateTime.minusDays(daysBefore), currentDateTime.plusDays(daysAfter))
  }
}

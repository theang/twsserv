package serv1.db

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import serv1.config.ServConfig
import serv1.db.exception.DatabaseException
import serv1.db.repo.intf.JobRepoIntf
import slick.util.Logging

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

object JobUpdateActor extends Logging {
  val config: Config = ServConfig.config.getConfig("databaseOperation")

  var jobUpdateBundleSize: Int = config.getInt("jobUpdateBundleSize")
  var jobUpdateMaxDelaySecond: Int = config.getInt("jobUpdateMaxDelaySecond")


  sealed trait ResultMessage

  case object Queued extends ResultMessage

  sealed trait Message

  case class JobUpdateMessage(jobId: UUID, tickerUpdates: List[JobRepoIntf.UpdateTickerJob]) extends Message

  case class Write(jobId: Seq[UUID]) extends Message

  case object Purge extends Message

  var queued: mutable.Map[UUID, ArrayBuffer[JobRepoIntf.UpdateTickerJob]] = mutable.HashMap().withDefault(_ => ArrayBuffer())

  case object TimerKey

  def apply(jobRepoIntf: JobRepoIntf): Behavior[Message] = {
    Behaviors.withTimers { timers =>
      timers.startTimerWithFixedDelay(TimerKey, Purge, FiniteDuration.apply(jobUpdateMaxDelaySecond, TimeUnit.SECONDS))
      Behaviors.setup[Message] {
        context =>
          Behaviors.receiveMessage {
            case JobUpdateMessage(jobId, tickerUpdate) =>
              queued(jobId).addAll(tickerUpdate)
              if (queued(jobId).length >= jobUpdateBundleSize) {
                context.self ! Write(Seq(jobId))
              }
              Behaviors.same
            case Write(jobIds) =>
              jobIds.foreach {
                jobId =>
                  val updates = queued(jobId)
                  if (updates.nonEmpty) {
                    try {
                      jobRepoIntf.updateJob(jobId, updates.toSeq)
                      queued(jobId).clear()
                    } catch {
                      case exc: DatabaseException =>
                        logger.warn(s"Blocking write unsuccessful id=$jobId updates=$updates exc=$exc")
                    }
                  }
              }
              Behaviors.same
            case Purge =>
              val jobIds = queued.toSeq.filter(_._2.nonEmpty).map(_._1)
              context.self ! Write(jobIds)
              Behaviors.same
          }
      }
    }
  }
}

package serv1.db

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.event.Logging
import com.typesafe.config.Config
import serv1.config.ServConfig
import serv1.db.repo.intf.TickerDataRepoIntf
import serv1.exception.DatabaseException
import serv1.model.HistoricalData
import serv1.model.ticker.TickerLoadType
import slick.util.Logging

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.{Duration, FiniteDuration, MILLISECONDS}
import scala.util.Random

object TickerDataActor extends Logging {
  val random: Random = Random
  val config: Config = ServConfig.config.getConfig("databaseOperation")
  val MAX_ATTEMPTS: Int = config.getInt("tickerRepoMaxAttempts")
  val tickerRepoConstantDelay: Int = config.getInt("tickerRepoConstantDelay")
  val tickerRepoRandomDelay: Int = config.getInt("tickerRepoRandomDelay")
  val timerKey: AtomicInteger = new AtomicInteger(1)

  sealed trait ResultMessage

  case object WriteSuccessful extends ResultMessage

  case object WriteFailed extends ResultMessage

  case class Write(attempt: Int,
                   ticker: TickerLoadType,
                   historicalData: Seq[HistoricalData],
                   replyTo: ActorRef[ResultMessage])

  def calculateRandomDelay(): FiniteDuration = {
    val msec = tickerRepoConstantDelay + random.nextInt(tickerRepoRandomDelay)
    Duration.create(msec, MILLISECONDS)
  }

  def blockingWrite(tickerDataRepo: TickerDataRepoIntf,
                    context: ActorContext[Write],
                    timers: TimerScheduler[Write],
                    timerKey: Int,
                    attempt: Int,
                    ticker: TickerLoadType,
                    historicalData: Seq[HistoricalData],
                    replyTo: ActorRef[ResultMessage]): Unit = {
    if (attempt > MAX_ATTEMPTS) {
      context.log.warn(s"Giving up for $ticker loading ${historicalData.size} items of historical data")
      if (replyTo != null) {
        replyTo ! WriteFailed
      }
    } else {
      try {
        logger.info(s"Blocking write $ticker")
        tickerDataRepo.write(ticker, historicalData)
        if (replyTo != null) {
          replyTo ! WriteSuccessful
        }
      } catch {
        case _: DatabaseException =>
          logger.info(s"Blocking write unsuccessful $ticker starting timer")
          timers.startSingleTimer(timerKey, Write(attempt + 1, ticker, historicalData, replyTo), calculateRandomDelay())
      }
    }
  }

  def apply(tickerDataRepo: TickerDataRepoIntf): Behavior[Write] = {
    Behaviors.withTimers { timers =>
      Behaviors.receive[Write] {
        case (context, Write(attempt, ticker, historicalData, replyTo)) =>
          blockingWrite(tickerDataRepo,
            context,
            timers,
            timerKey.getAndIncrement(),
            attempt,
            ticker,
            historicalData,
            replyTo)
          Behaviors.same
      }
    }
  }
}

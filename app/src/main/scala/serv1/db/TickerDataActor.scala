package serv1.db

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.config.Config
import serv1.config.ServConfig
import serv1.db.exception.DatabaseException
import serv1.db.repo.intf.{TickerDataRepoIntf, TickerTickRepoIntf}
import serv1.db.schema.{TickerTickBidAsk, TickerTickLast}
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

  sealed trait Message

  case class Write(attempt: Int,
                   ticker: TickerLoadType,
                   historicalData: Seq[HistoricalData],
                   overwrite: Boolean,
                   replyTo: ActorRef[ResultMessage]) extends Message

  case class WriteTickLast(attempt: Int,
                           ticker: TickerLoadType,
                           historicalData: Seq[TickerTickLast],
                           replyTo: ActorRef[ResultMessage]) extends Message

  case class WriteTickBidAsk(attempt: Int,
                             ticker: TickerLoadType,
                             historicalData: Seq[TickerTickBidAsk],
                             replyTo: ActorRef[ResultMessage]) extends Message

  case class WriteAction(attempt: Int,
                         ticker: TickerLoadType,
                         writeAction: () => Unit,
                         message: String,
                         replyTo: ActorRef[ResultMessage]) extends Message

  case class Retry(attempt: Int,
                   description: String,
                   action: () => Unit,
                   replyTo: ActorRef[ResultMessage]) extends Message

  def calculateRandomDelay(): FiniteDuration = {
    val msec = tickerRepoConstantDelay + random.nextInt(tickerRepoRandomDelay)
    Duration.create(msec, MILLISECONDS)
  }

  def tryWrite(context: ActorContext[Message],
               timers: TimerScheduler[Message],
               timerKey: Int,
               attempt: Int,
               writeAction: () => Unit,
               ticker: TickerLoadType,
               message: String,
               replyTo: ActorRef[ResultMessage]): Unit = {
    if (attempt > MAX_ATTEMPTS) {
      logger.warn(s"Giving up for $ticker $message")
      if (replyTo != null) {
        replyTo ! WriteFailed
      }
    } else {
      try {
        logger.debug(s"Blocking write $ticker")
        writeAction()
        if (replyTo != null) {
          replyTo ! WriteSuccessful
        }
      } catch {
        case _: DatabaseException =>
          logger.warn(s"Blocking write unsuccessful $ticker starting timer")
          timers.startSingleTimer(timerKey, WriteAction(attempt + 1, ticker, writeAction, message, replyTo), calculateRandomDelay())
      }
    }
  }

  def apply(tickerDataRepo: TickerDataRepoIntf, tickerTickRepo: TickerTickRepoIntf): Behavior[Message] = {
    Behaviors.withTimers { timers =>
      Behaviors.receive[Message] {
        case (context, Write(attempt, ticker, historicalData, overwrite, replyTo)) =>
          tryWrite(context,
            timers,
            timerKey.getAndIncrement(),
            attempt,
            () => {
              if (overwrite) {
                tickerDataRepo.writeUpdate(ticker, historicalData)
              } else {
                tickerDataRepo.write(ticker, historicalData)
              }
            },
            ticker,
            message = s"loading ${historicalData.size} items of historical data",
            replyTo)
          Behaviors.same
        case (context, WriteTickLast(attempt, ticker, tickLastData, replyTo)) =>
          tryWrite(context,
            timers,
            timerKey.getAndIncrement(),
            attempt,
            () => {
              tickerTickRepo.writeLast(ticker, tickLastData, checkAlreadyInDb = false)
            },
            ticker,
            message = s"loading ${tickLastData.size} items of tick last price data",
            replyTo)
          Behaviors.same
        case (context, WriteTickBidAsk(attempt, ticker, tickBidAskData, replyTo)) =>
          tryWrite(context,
            timers,
            timerKey.getAndIncrement(),
            attempt,
            () => {
              tickerTickRepo.writeBidAsk(ticker, tickBidAskData, checkAlreadyInDb = false)
            },
            ticker,
            message = s"loading ${tickBidAskData.size} items of tick bid-ask data",
            replyTo)
          Behaviors.same
        case (context, WriteAction(attempt, ticker, action, message, replyTo)) =>
          tryWrite(context,
            timers,
            timerKey.getAndIncrement(),
            attempt,
            action,
            ticker,
            message,
            replyTo)
          Behaviors.same
        case (context, Retry(attempt, description, action, replyTo)) =>
          if (attempt > MAX_ATTEMPTS) {
            logger.warn(s"Giving up for action $description")
            if (replyTo != null) {
              replyTo ! WriteFailed
            }
          } else {
            logger.debug(s"Execute action $description")
            action()
            if (replyTo != null) {
              replyTo ! WriteSuccessful
            }
          }
          Behaviors.same
      }
    }
  }
}

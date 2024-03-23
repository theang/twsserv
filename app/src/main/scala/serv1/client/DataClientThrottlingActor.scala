package serv1.client

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.typesafe.config.Config
import serv1.client.limiter.{SimultaneousLimiter, ThroughputLimiter}
import serv1.client.operations.{ClientOperationCallbacks, ClientOperationHandlers}
import serv1.config.ServConfig
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.HistoricalData
import serv1.model.ticker.TickerType
import slick.util.Logging

import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.util.control.Breaks.{break, breakable}

object DataClientThrottlingActor extends Logging {
  var config: Config = ServConfig.config.getConfig("throttlingActor")
  var identicalHistoricalRequestsCoolDownSeconds: Int = config.getInt("identicalHistoricalRequestsCoolDownSeconds")
  var simultaneousHistoricalRequests: Int = config.getInt("simultaneousHistoricalRequests")
  var defaultSizeLimit: Int = config.getInt("defaultSizeLimit")
  var sizeLimitMap: Map[Int, Int] = config.getConfigList("sizeLimitMap").asScala.map {
    config: Config => (config.getInt("barSize"), config.getInt("limit"))
  }.toMap
  var runRequestsFromQueueInterval: Int = config.getInt("runRequestsFromQueueInterval")
  var delayedQueueDelay: Int = config.getInt("delayedQueuesDelay")
  var sameTickerRequestsInterval: Int = config.getInt("sameTickerRequestsInterval")
  var sameTickerRequestsLimit: Int = config.getInt("sameTickerRequestsLimit")
  var throughputLimit: Int = config.getInt("throughputLimit")
  var throughputLimitSecond: Int = config.getInt("throughputLimitSecond")
  val REPORT_STATS_INTERVAL = 120

  sealed trait Message

  case class LoadHistoricalData(from: Long, to: Long, tickerType: TickerType, barSize: Int, historicalDataType: HistoricalDataType,
                                cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                                error: ClientOperationHandlers.ErrorHandler) extends Message

  case class RunFromQueue(typeKey: Option[String]) extends Message

  case object QueueDelayed extends Message

  case object ReportStats extends Message

  case object TimerKey

  case object DelayedTimerKey

  case object ReportStatsKey

  val simultaneousRequestLimiter = new SimultaneousLimiter(simultaneousHistoricalRequests)
  val totalRateLimiter = new ThroughputLimiter(throughputLimitSecond * 1000, throughputLimit)
  var currentRequestParts: mutable.Set[AtomicLong] = Collections.newSetFromMap[AtomicLong](new ConcurrentHashMap[AtomicLong, Boolean].asInstanceOf[java.util.Map[AtomicLong, java.lang.Boolean]]).asScala

  case class LoadHistoricalDataElement(counter: AtomicLong, loadHistoricalData: LoadHistoricalData)

  var loadHistoricalQueues: mutable.Map[String, mutable.Queue[LoadHistoricalDataElement]] = mutable.HashMap.empty
  val individualRateLimiters: mutable.Map[String, ThroughputLimiter] = mutable.HashMap.empty

  def calculateKey(loadHistoricalData: LoadHistoricalData): String = {
    List(loadHistoricalData.tickerType.name, loadHistoricalData.tickerType.exchange, loadHistoricalData.tickerType.typ, loadHistoricalData.historicalDataType.toString).mkString("-")
  }

  def getLimiterByType(typeKey: String): ThroughputLimiter = {
    individualRateLimiters.getOrElseUpdate(typeKey, new ThroughputLimiter(sameTickerRequestsInterval * 1000, sameTickerRequestsLimit))
  }

  def getAvailableRequestsByType(typeKey: String): Int = {
    getLimiterByType(typeKey).available
  }

  def incrementRequestsByType(typeKey: String): Unit = {
    getLimiterByType(typeKey).append()
  }

  def getQueueByKey(typeKey: String): mutable.Queue[LoadHistoricalDataElement] = {
    loadHistoricalQueues.getOrElseUpdate(typeKey, mutable.Queue[LoadHistoricalDataElement]())
  }

  def successfulLoading(typeKey: String, counter: AtomicLong, context: ActorContext[Message], cont: ClientOperationCallbacks.HistoricalDataOperationCallback, data: Seq[HistoricalData], last: Boolean): Unit = {
    if (last) {
      simultaneousRequestLimiter.detach
      if (currentRequestParts.contains(counter)) {
        val newValue = counter.decrementAndGet()
        if (newValue <= 0) {
          currentRequestParts.remove(counter)
          context.self ! RunFromQueue(Some(typeKey))
        }
      }
    }
    if (last && (counter.get() <= 0)) {
      logger.debug(s"Finalizing: $data")
    }
    cont(data, last && (counter.get() <= 0))
  }

  def errorLoading(typeKey: String, counter: AtomicLong, context: ActorContext[Message], error: ClientOperationHandlers.ErrorHandler, errorCode: Int, errorMessage: String, rejectionJson: String): Unit = {
    simultaneousRequestLimiter.detach
    if (currentRequestParts.contains(counter)) {
      val newValue = counter.decrementAndGet()
      if (newValue <= 0) {
        currentRequestParts.remove(counter)
        context.self ! RunFromQueue(Some(typeKey))
      }
    }
    error(errorCode, errorMessage, rejectionJson)
  }

  def startLoadingHistoricalData(counter: AtomicLong, dataClient: DataClient, context: ActorContext[Message], loadHistoricalData: LoadHistoricalData): Unit = loadHistoricalData match {
    case LoadHistoricalData(from, to, tickerType, barSize, historicalDataType, cont, error) =>
      val typeKey = calculateKey(loadHistoricalData)
      val contWrap = (data: Seq[HistoricalData], last: Boolean) => {
        successfulLoading(typeKey, counter, context, cont, data, last)
      }
      val errorWrap = (errorCode: Int, errorMessage: String, rejectionJson: String) => {
        errorLoading(typeKey, counter, context, error, errorCode, errorMessage, rejectionJson)
      }
      simultaneousRequestLimiter.append
      totalRateLimiter.append()
      incrementRequestsByType(typeKey)
      dataClient.loadHistoricalData(from, to, tickerType, barSize, historicalDataType, contWrap, errorWrap)
  }

  def makeManyRequests(loadHistoricalData: LoadHistoricalData): Seq[LoadHistoricalData] = {
    val sizeLimit = sizeLimitMap.getOrElse(loadHistoricalData.barSize, defaultSizeLimit)
    val chunkSize = loadHistoricalData.barSize * sizeLimit
    val segmentLength = loadHistoricalData.to - loadHistoricalData.from
    val fullIntervals = segmentLength / chunkSize
    val lastInterval = (fullIntervals * chunkSize) < segmentLength
    val totalIntervals = fullIntervals + (if (lastInterval) 1 else 0)
    (0 until totalIntervals.toInt).map { chunk =>
      val lastEpoch = loadHistoricalData.from + (chunk + 1) * chunkSize
      LoadHistoricalData(loadHistoricalData.from + chunk * chunkSize,
        if (lastEpoch > loadHistoricalData.to) loadHistoricalData.to else lastEpoch,
        loadHistoricalData.tickerType, loadHistoricalData.barSize, loadHistoricalData.historicalDataType,
        loadHistoricalData.cont, loadHistoricalData.error)
    }
  }

  def startFromQueue(queue: mutable.Queue[LoadHistoricalDataElement], typeKey: String, dataClient: DataClient, context: ActorContext[Message]): Unit = {
    val dequeueNumber = List(totalRateLimiter.available, getAvailableRequestsByType(typeKey), queue.size).min
    breakable {
      (0 until dequeueNumber).foreach {
        _ =>
          if (queue.nonEmpty && simultaneousRequestLimiter.available > 0) {
            queue.dequeue() match {
              case LoadHistoricalDataElement(counter, loadHistoricalData) =>
                startLoadingHistoricalData(counter, dataClient, context, loadHistoricalData)
            }
          } else {
            break
          }
      }
    }
  }

  def startByTypeKey(typeKey: String, dataClient: DataClient, context: ActorContext[Message]): Unit = {
    if (getAvailableRequestsByType(typeKey) > 0) {
      val queue = getQueueByKey(typeKey)
      startFromQueue(queue, typeKey, dataClient, context)
    }
  }

  def startAllAvailable(dataClient: DataClient, context: ActorContext[Message]): Unit = {
    if (totalRateLimiter.available > 0 && simultaneousRequestLimiter.available > 0) {
      breakable {
        loadHistoricalQueues.foreach {
          case (typeKey, queue) =>
            if (totalRateLimiter.available == 0) {
              break
            }
            if (queue.nonEmpty) {
              startFromQueue(queue, typeKey, dataClient, context)
            }
        }
      }
    }
  }

  def apply(dataClient: DataClient): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        timers.startTimerWithFixedDelay(TimerKey, RunFromQueue(None), FiniteDuration.apply(runRequestsFromQueueInterval, TimeUnit.SECONDS), FiniteDuration.apply(runRequestsFromQueueInterval, TimeUnit.SECONDS))
        timers.startTimerWithFixedDelay(ReportStatsKey, ReportStats, FiniteDuration.apply(REPORT_STATS_INTERVAL, TimeUnit.SECONDS), FiniteDuration.apply(REPORT_STATS_INTERVAL, TimeUnit.SECONDS))
        Behaviors.setup[Message] {
          context =>
            Behaviors.receiveMessage {
              case loadHistoricalData: LoadHistoricalData =>
                val manyRequests = makeManyRequests(loadHistoricalData).reverse
                val counter = new AtomicLong(manyRequests.size)
                currentRequestParts.add(counter)

                val elements = manyRequests.map(LoadHistoricalDataElement(counter, _))

                val typeKey = calculateKey(loadHistoricalData)
                val availableByType = Math.min(getAvailableRequestsByType(typeKey), totalRateLimiter.available)

                val queue = loadHistoricalQueues.getOrElseUpdate(typeKey, mutable.Queue[LoadHistoricalDataElement]())
                queue.enqueueAll(elements)
                if (availableByType > 0)
                  context.self ! RunFromQueue(Some(typeKey))
                logger.debug(s"added to queue: $loadHistoricalData $manyRequests $counter $currentRequestParts")
                Behaviors.same
              case RunFromQueue(typeKeyOpt) =>
                logger.debug(s"run from queue: $currentRequestParts")
                if (totalRateLimiter.available > 0 && simultaneousRequestLimiter.available > 0) {
                  if (typeKeyOpt.isDefined) {
                    val typeKey = typeKeyOpt.get
                    startByTypeKey(typeKey, dataClient, context)
                  }
                  if (totalRateLimiter.available > 0 && simultaneousRequestLimiter.available > 0) {
                    startAllAvailable(dataClient, context)
                  }
                }
                Behaviors.same
              case ReportStats =>
                logger.info(s"DataClientThrottlingActor: request statistics: queueSize = ${loadHistoricalQueues.map(_._2.size).sum}, currentRequestsParts.sum = ${currentRequestParts.map(_.get()).sum}, currentRequestsParts.size = ${currentRequestParts.size}")
                Behaviors.same
            }
        }
    }
  }
}

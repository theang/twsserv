package serv1.client

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.typesafe.config.Config
import serv1.client.operations.{ClientOperationCallbacks, ClientOperationHandlers}
import serv1.config.ServConfig
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.HistoricalData
import serv1.model.ticker.TickerType
import slick.util.Logging

import java.util.Collections
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

object DataClientThrottlingActor extends Logging {
  var config: Config = ServConfig.config.getConfig("throttlingActor")
  var identicalHistoricalRequestsCoolDownSeconds = config.getInt("identicalHistoricalRequestsCoolDownSeconds")
  var simultaneousHistoricalRequests = config.getInt("simultaneousHistoricalRequests")
  var sizeLimit = config.getInt("sizeLimit")
  var runRequestsFromQueueInterval = config.getInt("runRequestsFromQueueInterval")

  sealed trait Message

  case class LoadHistoricalData(from: Long, to: Long, tickerType: TickerType, barSize: Int, historicalDataType: HistoricalDataType,
                                cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                                error: ClientOperationHandlers.ErrorHandler) extends Message

  case object RunFromQueue extends Message


  case object TimerKey

  var currentRequests: AtomicInteger = new AtomicInteger()
  var currentRequestParts: mutable.Set[AtomicLong] = Collections.newSetFromMap[AtomicLong](new ConcurrentHashMap[AtomicLong, Boolean].asInstanceOf[java.util.Map[AtomicLong, java.lang.Boolean]]).asScala

  case class LoadHistoricalDataElement(counter: AtomicLong, loadHistoricalData: LoadHistoricalData)

  var loadHistoricalDataQueue: mutable.Queue[LoadHistoricalDataElement] = mutable.Queue[LoadHistoricalDataElement]()

  def successfulLoading(counter: AtomicLong, context: ActorContext[Message], cont: ClientOperationCallbacks.HistoricalDataOperationCallback, data: Seq[HistoricalData], last: Boolean): Unit = {
    if (last) {
      currentRequests.decrementAndGet()
      if (currentRequestParts.contains(counter)) {
        val newValue = counter.decrementAndGet()
        if (newValue == 0) {
          currentRequestParts.remove(counter)
          context.self ! RunFromQueue
        }
      }
    }
    if (last && (counter.get() <= 0)) {
      logger.info(s"Finalizing: $data")
    }
    cont(data, last && (counter.get() <= 0))
  }

  def errorLoading(counter: AtomicLong, context: ActorContext[Message], error: ClientOperationHandlers.ErrorHandler, errorCode: Int, errorMessage: String, rejectionJson: String): Unit = {
    currentRequests.decrementAndGet()
    if (currentRequestParts.contains(counter)) {
      val newValue = counter.decrementAndGet()
      if (newValue == 0) {
        currentRequestParts.remove(counter)
        context.self ! RunFromQueue
      }
    }
    error(errorCode, errorMessage, rejectionJson)
  }

  def startLoadingHistoricalData(counter: AtomicLong, dataClient: DataClient, context: ActorContext[Message], loadHistoricalData: LoadHistoricalData): Unit = loadHistoricalData match {
    case LoadHistoricalData(from, to, tickerType, barSize, historicalDataType, cont, error) =>
      val contWrap = (data: Seq[HistoricalData], last: Boolean) => {
        successfulLoading(counter, context, cont, data, last)
      }
      val errorWrap = (errorCode: Int, errorMessage: String, rejectionJson: String) => {
        errorLoading(counter, context, error, errorCode, errorMessage, rejectionJson)
      }
      currentRequests.incrementAndGet()
      dataClient.loadHistoricalData(from, to, tickerType, barSize, historicalDataType, contWrap, errorWrap)
  }

  def makeManyRequests(loadHistoricalData: LoadHistoricalData): Seq[LoadHistoricalData] = {
    val chunkSize = loadHistoricalData.barSize * sizeLimit
    val segmentLength = loadHistoricalData.to - loadHistoricalData.from
    val fullIntervals = segmentLength / chunkSize
    val lastInterval = (fullIntervals * chunkSize) < segmentLength
    val totalIntervals = fullIntervals + (if (lastInterval) 1 else 0)
    (0 until totalIntervals.toInt).map { chunk =>
      val lastEpoch = loadHistoricalData.from + (chunk + 1) * chunkSize
      LoadHistoricalData(loadHistoricalData.from + chunk * chunkSize,
        (if (lastEpoch > loadHistoricalData.to) loadHistoricalData.to else lastEpoch),
        loadHistoricalData.tickerType, loadHistoricalData.barSize, loadHistoricalData.historicalDataType,
        loadHistoricalData.cont, loadHistoricalData.error)
    }
  }

  def apply(dataClient: DataClient): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        timers.startTimerWithFixedDelay(TimerKey, RunFromQueue, FiniteDuration.apply(runRequestsFromQueueInterval, TimeUnit.SECONDS), FiniteDuration.apply(runRequestsFromQueueInterval, TimeUnit.SECONDS))
        Behaviors.setup[Message] {
          context =>
            Behaviors.receiveMessage {
              case loadHistoricalData: LoadHistoricalData =>
                val manyRequests = makeManyRequests(loadHistoricalData).reverse
                val counter = new AtomicLong(manyRequests.size)
                currentRequestParts.add(counter)
                loadHistoricalDataQueue.enqueueAll(manyRequests.map(LoadHistoricalDataElement(counter, _)))
                if (currentRequests.get() < simultaneousHistoricalRequests)
                  context.self ! RunFromQueue
                Behaviors.same
              case RunFromQueue =>
                while (loadHistoricalDataQueue.nonEmpty && (currentRequests.get() < simultaneousHistoricalRequests)) {
                  loadHistoricalDataQueue.dequeue() match {
                    case LoadHistoricalDataElement(counter, loadHistoricalData) =>
                      startLoadingHistoricalData(counter, dataClient, context, loadHistoricalData)
                  }
                }
                Behaviors.same
            }
        }
    }
  }
}

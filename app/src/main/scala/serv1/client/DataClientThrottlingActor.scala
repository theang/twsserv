package serv1.client

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config
import serv1.client.operations.{ClientOperationCallbacks, ClientOperationHandlers}
import serv1.config.ServConfig
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.ticker.TickerType

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable

class DataClientThrottlingActor(dataClient: DataClient) {
  var config: Config = ServConfig.config.getConfig("throttlingActor")
  var identicalHistoricalRequestsCoolDownSeconds = config.getInt("identicalHistoricalRequestsCoolDownSeconds")
  var simultaneousHistoricalRequests = config.getInt("simultaneousHistoricalRequests")
  var sizeLimit = config.getInt("sizeLimit")

  sealed trait Message

  case class LoadHistoricalData(from: Long, to: Long, tickerType: TickerType, barSize: Int, historicalDataType: HistoricalDataType,
                                cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                                error: ClientOperationHandlers.ErrorHandler) extends Message

  case object RunFromQueue extends Message

  case object TimerKey

  var currentRequests: AtomicInteger = new AtomicInteger()
  var loadHistoricalDataQueue: mutable.Queue[LoadHistoricalData] = mutable.Queue[LoadHistoricalData]()

  def startLoadingHistoricalData(loadHistoricalData: LoadHistoricalData): Unit = loadHistoricalData match {
    case LoadHistoricalData(from, to, tickerType, barSize, historicalDataType, cont, error) =>
      currentRequests.incrementAndGet()
      dataClient.loadHistoricalData(from, to, tickerType, barSize, historicalDataType, cont, error)
  }

  def makeSmallerRequests(loadHistoricalData: LoadHistoricalData) = {
    
  }

  def apply(): Behavior[Message] = {
    Behaviors.withTimers {
      timers =>
        Behaviors.receive[Message] {
          case (_, loadHistoricalData: LoadHistoricalData) =>
            if (currentRequests.get() > simultaneousHistoricalRequests)
              loadHistoricalDataQueue.enqueue(loadHistoricalData)
            else
              startLoadingHistoricalData(loadHistoricalData)
            Behaviors.same
          case (_, RunFromQueue) =>
            while (loadHistoricalDataQueue.nonEmpty && (currentRequests.get() < simultaneousHistoricalRequests)) {
              startLoadingHistoricalData(loadHistoricalDataQueue.dequeue())
            }
            Behaviors.same
        }
    }
  }
}

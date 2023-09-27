package serv1.client.operations

import com.ib.client.Bar
import serv1.client.TWSClient.config
import serv1.client.model.TickerTickLastExchange
import serv1.db.schema.TickerTickBidAsk
import serv1.model.HistoricalData
import slick.util.Logging

import java.util.concurrent.{ConcurrentHashMap, Executors}
import scala.collection.concurrent
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.jdk.CollectionConverters._

object ClientOperationHandlers extends Logging {
  var THREADS_NUMBER: Int = 5
  implicit val context: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(THREADS_NUMBER))
  var LOAD_CHUNK_SIZE: Int = config.getInt("loadChunkSize")
  var LOAD_TICK_CHUNK_SIZE: Int = config.getInt("loadTickChunkSize")
  type ErrorHandler = (Int, String, String) => Unit

  case class TickerLastDataOperation(clOp: ClientOperation[TickerTickLastExchange, ArrayBuffer[TickerTickLastExchange], ClientOperationCallbacks.TickLastOperationCallback]) extends DataOperation

  case class TickerBidAskDataOperation(clOp: ClientOperation[TickerTickBidAsk, ArrayBuffer[TickerTickBidAsk], ClientOperationCallbacks.TickBidAskOperationCallback]) extends DataOperation

  val historicalData: concurrent.Map[Int, DataOperation] =
    new ConcurrentHashMap[Int, DataOperation]().asScala
  val errorHandlers: concurrent.Map[Int, ErrorHandler] =
    new ConcurrentHashMap[Int, ErrorHandler]().asScala

  def removeHandlers(handlerN: Int): Unit = {
    historicalData.remove(handlerN)
    errorHandlers.remove(handlerN)
  }

  def addHistoricalDataHandler(reqN: Int,
                               precMultiplier: Int,
                               dateFormat: Int,
                               cont: ClientOperationCallbacks.HistoricalDataOperationCallback,
                               error: ErrorHandler): Unit = {
    historicalData.addOne((reqN,
      new HistoricalDataClientOperation(cont, new ArrayBuffer[HistoricalData](), precMultiplier, dateFormat, LOAD_CHUNK_SIZE, false)))
    errorHandlers.addOne((reqN, error))
  }

  def addTickLastDataHandler(reqN: Int,
                             cont: ClientOperationCallbacks.TickLastOperationCallback,
                             error: ErrorHandler): Unit = {
    historicalData.addOne((reqN,
      new TickLastDataClientOperation(cont, new ArrayBuffer[TickerTickLastExchange](), LOAD_TICK_CHUNK_SIZE)))
    errorHandlers.addOne((reqN, error))
  }

  def addTickBidAskDataHandler(reqN: Int,
                               cont: ClientOperationCallbacks.TickBidAskOperationCallback,
                               error: ErrorHandler): Unit = {
    historicalData.addOne((reqN,
      new TickBidAskDataClientOperation(cont, new ArrayBuffer[TickerTickBidAsk](), LOAD_TICK_CHUNK_SIZE)))
    errorHandlers.addOne((reqN, error))
  }

  def purgeData(reqN: Int): Unit = {
    historicalData.get(reqN) match {
      case Some(dataOp) =>
        dataOp match {
          case histDataOp: ClientOperationAdder[?] =>
            if (histDataOp.getSize > 0 && histDataOp.purgeable) {
              histDataOp.synchronized {
                if (histDataOp.getSize > 0) {
                  histDataOp.executeCont(false)
                }
              }
            }
          case _ => logger.warn(s"purgeData: reqN = $reqN;" +
            s" no ClientOperationAdder found skipping")
        }
      case None =>
        logger.warn(s"purgeHistoricalData: reqN = $reqN;" +
          s" no request found by number skipping")
    }
  }

  def purgeAllData(): Unit = {
    historicalData.foreachEntry {
      (ind, _) => purgeData(ind)
    }
  }
  def handleData[DatumType](reqN: Int, datum: DatumType, last: Boolean): Unit = {
    historicalData.get(reqN) match {
      case Some(dataOp) =>
        dataOp match {
          case histDataOp: ClientOperationAdder[DatumType] =>
            histDataOp.synchronized {
              if (datum != null) {
                histDataOp.addOne(datum)
              }
              if ((histDataOp.getSize >= histDataOp.chunkSize) || last) {
                histDataOp.executeCont(last)
              }
            }
            if (last) {
              removeHandlers(reqN)
            }
          case _ => logger.warn(s"handleData: reqN = $reqN;" +
            s" no ClientOperationAdder found")
        }
      case None =>
        logger.warn(s"handleHistoricalData: reqN = $reqN;" +
          s" no request found by number")
    }
  }

  def handleHistoricalData(reqN: Int, bar: Bar, last: Boolean): Unit = {
    handleData(reqN, bar, last)
  }

  def handleError(reqN: Int, code: Int, msg: String, advancedOrderRejectJson: String): Unit = {
    errorHandlers.get(reqN) match {
      case Some(errH) =>
        errH(code, msg, advancedOrderRejectJson)
      case None =>
        logger.warn(s"handleError: reqN = $reqN, code = $code, msg = $msg, advancedOrderRejectJson = $advancedOrderRejectJson;" +
          s" no request found by number")
    }
  }
}

package serv1.client.operations

import com.ib.client.Bar
import serv1.client.TWSClient.config
import serv1.client.converters.HistoricalDataConverter
import serv1.model.HistoricalData
import slick.util.Logging

import java.util.concurrent.{ConcurrentHashMap, Executors}
import scala.collection.concurrent
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._

object ClientOperationHandlers extends Logging {
  val THREADS_NUMBER = 5
  implicit val context: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(THREADS_NUMBER))
  val LOAD_CHUNK_SIZE: Int = config.getInt("loadChunkSize")
  type ErrorHandler = (Int, String) => Unit

  type HistoricalDataOperationCallback = (Seq[HistoricalData], Boolean) => Unit
  case class HistoricalDataOperation(barConv: Bar => HistoricalData,
                                     clOp: ClientOperation[ArrayBuffer[HistoricalData], HistoricalDataOperationCallback])

  val historicalData: concurrent.Map[Int, HistoricalDataOperation] =
    new ConcurrentHashMap[Int, HistoricalDataOperation]().asScala
  val errorHandlers: concurrent.Map[Int, ErrorHandler] =
    new ConcurrentHashMap[Int, ErrorHandler]().asScala

  def removeHandlers(handlerN: Int): Unit = {
    historicalData.remove(handlerN)
    errorHandlers.remove(handlerN)
  }
  def addHistoricalDataHandler(reqN: Int,
                               precMultiplier: Int,
                               dateFormat: Int,
                               cont: HistoricalDataOperationCallback,
                               error: ErrorHandler): Unit = {
    val clOp = ClientOperation[ArrayBuffer[HistoricalData], HistoricalDataOperationCallback](cont, new ArrayBuffer[HistoricalData]())
    historicalData.addOne((reqN,
      HistoricalDataOperation(HistoricalDataConverter.fromPrecAndDateFormat(precMultiplier, dateFormat),
        clOp)))
    errorHandlers.addOne((reqN, error))
  }

  def handleHistoricalData(reqN: Int, bar: Bar, last: Boolean): Unit = {
    historicalData.get(reqN) match {
      case Some(co) =>
        co.clOp.data.synchronized {
          if (bar != null) {
            co.clOp.data.addOne(co.barConv(bar))
          }
          if ((co.clOp.data.size >= LOAD_CHUNK_SIZE) || last) {
            val list = co.clOp.data.toList
            Future {
              co.clOp.cont(list, last)
            }
            co.clOp.data.clear()
          }
        }
        if (last) {
          removeHandlers(reqN)
        }
      case None =>
        logger.warn(s"handleHistoricalData: reqN = $reqN;" +
          s" no request found by number")
    }
  }

  def handleError(reqN: Int, code: Int, msg: String): Unit = {
    errorHandlers.get(reqN) match {
      case Some(errH) =>
        errH(code, msg)
      case None =>
        logger.warn(s"handleError: reqN = $reqN, code = $code, msg = $msg;" +
          s" no request found by number")
    }
  }
}

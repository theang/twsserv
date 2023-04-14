package serv1.client.operations

import com.ib.client.Bar
import serv1.client.converters.HistoricalDataConverter
import serv1.client.model.TickerTickLastExchange
import serv1.db.schema.TickerTickBidAsk
import serv1.model.HistoricalData

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

trait ClientOperationAdder[DataType] {
  val chunkSize: Int

  def addOne(datum: DataType): Unit

  def getSize: Int

  def executeCont(last: Boolean): Unit
}

abstract case class ClientOperation[DataType, AccType, Callback](contP: Callback, dataP: AccType) extends ClientOperationAdder[DataType] {
  var cont: Callback = contP
  var data: AccType = dataP
}

object ClientOperationCallbacks {
  type HistoricalDataOperationCallback = (Seq[HistoricalData], Boolean) => Unit

  type TickLastOperationCallback = (Seq[TickerTickLastExchange], Boolean) => Unit

  type TickBidAskOperationCallback = (Seq[TickerTickBidAsk], Boolean) => Unit
}

trait DataOperation

class HistoricalDataClientOperation(contP: ClientOperationCallbacks.HistoricalDataOperationCallback,
                                    dataP: ArrayBuffer[HistoricalData], precMultiplier: Int,
                                    dateFormat: Int, pChunkSize: Int)(implicit executionContext: ExecutionContext)
  extends ClientOperation[Bar, ArrayBuffer[HistoricalData], ClientOperationCallbacks.HistoricalDataOperationCallback](contP, dataP) with DataOperation {

  private val converter = HistoricalDataConverter.fromPrecAndDateFormat(precMultiplier, dateFormat)

  override def addOne(datum: Bar): Unit = {
    data.addOne(converter(datum))
  }


  override def executeCont(last: Boolean): Unit = {
    val list = data.toList
    Future {
      cont(list, last)
    }
    data.clear()
  }

  override val chunkSize: Int = pChunkSize

  override def getSize: Int = {
    data.size
  }
}

class TickLastDataClientOperation(contP: ClientOperationCallbacks.TickLastOperationCallback, dataP: ArrayBuffer[TickerTickLastExchange],
                                  pChunkSize: Int)(implicit executionContext: ExecutionContext)
  extends ClientOperation[TickerTickLastExchange, ArrayBuffer[TickerTickLastExchange], ClientOperationCallbacks.TickLastOperationCallback](contP, dataP) with DataOperation {

  override def addOne(datum: TickerTickLastExchange): Unit = {
    data.addOne(datum)
  }

  override def executeCont(last: Boolean): Unit = {
    val list = data.toList
    Future {
      cont(list, last)
    }
    data.clear()
  }

  override val chunkSize: Int = pChunkSize

  override def getSize: Int = {
    data.size
  }
}

class TickBidAskDataClientOperation(contP: ClientOperationCallbacks.TickBidAskOperationCallback, dataP: ArrayBuffer[TickerTickBidAsk],
                                    pChunkSize: Int)(implicit executionContext: ExecutionContext)
  extends ClientOperation[TickerTickBidAsk, ArrayBuffer[TickerTickBidAsk], ClientOperationCallbacks.TickBidAskOperationCallback](contP, dataP) with DataOperation {

  override def addOne(datum: TickerTickBidAsk): Unit = {
    data.addOne(datum)
  }

  override def executeCont(last: Boolean): Unit = {
    val list = data.toList
    Future {
      cont(list, last)
    }
    data.clear()
  }

  override val chunkSize: Int = pChunkSize

  override def getSize: Int = {
    data.size
  }
}
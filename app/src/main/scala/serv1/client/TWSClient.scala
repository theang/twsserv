package serv1.client

import com.ib.client._
import com.typesafe.config.Config
import serv1.client.converters.{BarSizeConverter, ContractConverter}
import serv1.client.operations.ClientOperationHandlers
import serv1.config.ServConfig
import serv1.util.LocalDateTimeUtil
import slick.util.Logging

import java.time.format.DateTimeFormatter
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger
import java.{lang, util}

object TWSClient extends DataClient with EWrapper with Logging {
  var config: Config = ServConfig.config.getConfig("twsClient")
  var signal = new EJavaSignal
  var client: EClientSocket = new EClientSocket(this, signal)
  var reader = new EReader(client, signal)
  val readerThreadState = new AtomicInteger(1)
  var thread: Thread = new Thread {
    override def run(): Unit = {
      while (readerThreadState.get() == 1) {
        if (client.isConnected) {
          logger.debug("client is connected")
          // it is possible we have messages
          reader.processMsgs()
          while (client.isConnected) {
            logger.debug("while client is connected")
            signal.waitForSignal()
            try {
              logger.debug("processing messages")
              reader.processMsgs()
              logger.debug("processed")
            } catch {
              case exc: Exception => error(exc)
            }
          }
        } else {
          readerThreadState.synchronized {
            logger.debug("client is not connected")
            // wait connection setup
            if (!client.isConnected) {
              logger.debug("client is not connected, waiting in sync block for client notification")
              readerThreadState.wait()
              logger.debug("signalled to loop in reader thread")
            }
          }
        }
      }
    }
  }
  {
    thread.start()
  }

  def dropIsConnectedFlagIfClientNotConnected(): Unit = {
    if ((!client.isConnected) && (isConnected.get() != 0)) {
      this.synchronized {
        if ((!client.isConnected) && (isConnected.get() != 0)) {
          isConnected.set(0)
        }
      }
    }
  }

  def checkOrderSequence(): Unit = {
    if (orderSequence.get() == 0) {
      logger.debug("order sequence is not set, meaning nextValidId was not yet called")
      //sequence is not set yet
      orderSequence.synchronized {
        logger.debug("sync on order sequence")
        //check here after blocking
        if (orderSequence.get() == 0) {
          logger.debug("sync is not yet set")
          logger.debug("Call client reqIds")
          client.reqIds(-1)
          //wait nextId to set sequence
          logger.debug("block wait")
          orderSequence.wait()
          logger.debug("wait is done, order sequence is set here, set connected")
        } else {
          //already set
          logger.debug("order sequence already set here")
        }
      }
    } else {
      logger.debug("order sequence is not zero, set connected")
    }
  }

  def connect(): Unit = {
    if (!client.isConnected) {
      logger.debug("client is not connected")
      //if client not connected
      //clear sequence
      sequence.set(0)
      logger.debug("reset sequence")

      //call connect asynchronously
      logger.debug("start connecting")
      client.eConnect(config.getString("host"),
        config.getString("port").toInt,
        config.getString("clientId").toInt)
      reader = new EReader(client, signal)
      readerThreadState.synchronized {
        readerThreadState.notify()
      }
      logger.debug("connection established")
      isConnected.set(if (client.isConnected) 1 else 0)
    } else {
      //it seems isConnected wrongly set to 0
      logger.debug("client is connected, isConnected is wrongly set to 0")
      isConnected.set(1)
    }
  }

  def checkConnected(): Unit = {
    try {
      //drop isConnected if discover not connected
      dropIsConnectedFlagIfClientNotConnected()

      if (isConnected.get() == 0) {
        //not connected
        logger.debug("not connected")
        this.synchronized {
          logger.debug("synchronized on this")
          //was connected before synchronized?
          if (isConnected.get() == 0) {
            logger.debug("still not connected")
            //not connected
            connect()
          }
        }
      }
    } catch {
      case exc: InterruptedException =>
        logger.error("Interrupted while waiting for connection", exc)
        throw new RuntimeException("Thread is interrupted", exc)
    }
  }

  val isConnected = new AtomicInteger(0)
  val sequence = new AtomicInteger(1)
  val orderSequence = new AtomicInteger(0)
  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss")


  def loadHistoricalData(from: Long, to: Long, ticker: String, exchange: String, typ: String, barSize: Int, prec: Int,
                         cont: ClientOperationHandlers.HistoricalDataOperationCallback,
                         error: ClientOperationHandlers.ErrorHandler): Unit = {
    checkConnected()

    val contract = ContractConverter.getContract(ticker, exchange, typ)
    val reqN = sequence.getAndIncrement()
    val queryTime = LocalDateTimeUtil.fromEpoch(to).format(formatter)
    val duration = s"${(to - from) / 1000} S"
    val barSizeStr = BarSizeConverter.getBarSize(barSize)
    val dateFormat = BarSizeConverter.getDateFormat(barSize)
    logger.debug(s"historicalData request: $reqN: $contract, $queryTime, $duration, $barSizeStr")
    ClientOperationHandlers.addHistoricalDataHandler(reqN, prec, dateFormat, cont, error)
    client.reqHistoricalData(reqN, contract, queryTime, duration, barSizeStr, "TRADES", 1, dateFormat, false, null)
  }

  override def tickPrice(i: Int, i1: Int, v: Double, tickAttrib: TickAttrib): Unit = ???

  override def tickSize(i: Int, i1: Int, i2: Int): Unit = ???

  override def tickOptionComputation(i: Int, i1: Int, i2: Int, v: Double, v1: Double, v2: Double, v3: Double, v4: Double, v5: Double, v6: Double, v7: Double): Unit = ???

  override def tickGeneric(i: Int, i1: Int, v: Double): Unit = ???

  override def tickString(i: Int, i1: Int, s: String): Unit = ???

  override def tickEFP(i: Int, i1: Int, v: Double, s: String, v1: Double, i2: Int, s1: String, v2: Double, v3: Double): Unit = ???

  override def orderStatus(i: Int, s: String, v: Double, v1: Double, v2: Double, i1: Int, i2: Int, v3: Double, i3: Int, s1: String, v4: Double): Unit = ???

  override def openOrder(i: Int, contract: Contract, order: Order, orderState: OrderState): Unit = ???

  override def openOrderEnd(): Unit = ???

  override def updateAccountValue(s: String, s1: String, s2: String, s3: String): Unit = ???

  override def updatePortfolio(contract: Contract, v: Double, v1: Double, v2: Double, v3: Double, v4: Double, v5: Double, s: String): Unit = ???

  override def updateAccountTime(s: String): Unit = ???

  override def accountDownloadEnd(s: String): Unit = ???

  override def nextValidId(i: Int): Unit = {
    //wait here until allowed to set
    //to synchronize with checkConnected method
    logger.debug("nextValidId: $i")
    orderSequence.synchronized {
      logger.debug("setting in sync block")
      orderSequence.set(i)
      logger.debug("notify thread to unblock connection thread")
      orderSequence.notify()
    }
  }

  override def contractDetails(i: Int, contractDetails: ContractDetails): Unit = ???

  override def bondContractDetails(i: Int, contractDetails: ContractDetails): Unit = ???

  override def contractDetailsEnd(i: Int): Unit = ???

  override def execDetails(i: Int, contract: Contract, execution: Execution): Unit = ???

  override def execDetailsEnd(i: Int): Unit = ???

  override def updateMktDepth(i: Int, i1: Int, i2: Int, i3: Int, v: Double, i4: Int): Unit = ???

  override def updateMktDepthL2(i: Int, i1: Int, s: String, i2: Int, i3: Int, v: Double, i4: Int, b: Boolean): Unit = ???

  override def updateNewsBulletin(i: Int, i1: Int, s: String, s1: String): Unit = ???

  override def managedAccounts(s: String): Unit = ???

  override def receiveFA(i: Int, s: String): Unit = ???

  override def historicalData(i: Int, bar: Bar): Unit = {
    logger.debug(s"historical data: $i")
    ClientOperationHandlers.handleHistoricalData(i, bar, last = false)
  }

  override def historicalDataEnd(i: Int, s: String, s1: String): Unit = {
    logger.debug(s"Historical end: $i, $s, $s1")
    ClientOperationHandlers.handleHistoricalData(i, null, last = true)
  }


  override def scannerParameters(s: String): Unit = ???

  override def scannerData(i: Int, i1: Int, contractDetails: ContractDetails, s: String, s1: String, s2: String, s3: String): Unit = ???

  override def scannerDataEnd(i: Int): Unit = ???

  override def realtimeBar(i: Int, l: Long, v: Double, v1: Double, v2: Double, v3: Double, l1: Long, v4: Double, i1: Int): Unit = ???

  override def currentTime(l: Long): Unit = ???

  override def fundamentalData(i: Int, s: String): Unit = ???

  override def deltaNeutralValidation(i: Int, deltaNeutralContract: DeltaNeutralContract): Unit = ???

  override def tickSnapshotEnd(i: Int): Unit = ???

  override def marketDataType(i: Int, i1: Int): Unit = ???

  override def commissionReport(commissionReport: CommissionReport): Unit = ???

  override def position(s: String, contract: Contract, v: Double, v1: Double): Unit = ???

  override def positionEnd(): Unit = ???

  override def accountSummary(i: Int, s: String, s1: String, s2: String, s3: String): Unit = ???

  override def accountSummaryEnd(i: Int): Unit = ???

  override def verifyMessageAPI(s: String): Unit = ???

  override def verifyCompleted(b: Boolean, s: String): Unit = ???

  override def verifyAndAuthMessageAPI(s: String, s1: String): Unit = ???

  override def verifyAndAuthCompleted(b: Boolean, s: String): Unit = ???

  override def displayGroupList(i: Int, s: String): Unit = ???

  override def displayGroupUpdated(i: Int, s: String): Unit = ???

  override def error(e: Exception): Unit = {
    logger.error("TWSClient: Exception in client", e)
  }

  override def error(s: String): Unit = {
    logger.warn(s"TWSClient warning: $s")
  }

  override def error(i: Int, i1: Int, s: String): Unit = {
    logger.debug(s"error: $i, $i1, $s")
    ClientOperationHandlers.handleError(i, i1, s)
  }

  override def connectionClosed(): Unit = ???

  override def connectAck(): Unit = {
    readerThreadState.synchronized {
      logger.debug("connectAck")
      readerThreadState.notify()
    }
  }

  override def positionMulti(i: Int, s: String, s1: String, contract: Contract, v: Double, v1: Double): Unit = ???

  override def positionMultiEnd(i: Int): Unit = ???

  override def accountUpdateMulti(i: Int, s: String, s1: String, s2: String, s3: String, s4: String): Unit = ???

  override def accountUpdateMultiEnd(i: Int): Unit = ???

  override def securityDefinitionOptionalParameter(i: Int, s: String, i1: Int, s1: String, s2: String, set: util.Set[String], set1: util.Set[lang.Double]): Unit = ???

  override def securityDefinitionOptionalParameterEnd(i: Int): Unit = ???

  override def softDollarTiers(i: Int, softDollarTiers: Array[SoftDollarTier]): Unit = ???

  override def familyCodes(familyCodes: Array[FamilyCode]): Unit = ???

  override def symbolSamples(i: Int, contractDescriptions: Array[ContractDescription]): Unit = ???

  override def mktDepthExchanges(depthMktDataDescriptions: Array[DepthMktDataDescription]): Unit = ???

  override def tickNews(i: Int, l: Long, s: String, s1: String, s2: String, s3: String): Unit = ???

  override def smartComponents(i: Int, map: util.Map[Integer, util.Map.Entry[String, Character]]): Unit = ???

  override def tickReqParams(i: Int, v: Double, s: String, i1: Int): Unit = ???

  override def newsProviders(newsProviders: Array[NewsProvider]): Unit = ???

  override def newsArticle(i: Int, i1: Int, s: String): Unit = ???

  override def historicalNews(i: Int, s: String, s1: String, s2: String, s3: String): Unit = ???

  override def historicalNewsEnd(i: Int, b: Boolean): Unit = ???

  override def headTimestamp(i: Int, s: String): Unit = ???

  override def histogramData(i: Int, list: util.List[HistogramEntry]): Unit = ???

  override def historicalDataUpdate(i: Int, bar: Bar): Unit = ???

  override def rerouteMktDataReq(i: Int, i1: Int, s: String): Unit = ???

  override def rerouteMktDepthReq(i: Int, i1: Int, s: String): Unit = ???

  override def marketRule(i: Int, priceIncrements: Array[PriceIncrement]): Unit = ???

  override def pnl(i: Int, v: Double, v1: Double, v2: Double): Unit = ???

  override def pnlSingle(i: Int, i1: Int, v: Double, v1: Double, v2: Double, v3: Double): Unit = ???

  override def historicalTicks(i: Int, list: util.List[HistoricalTick], b: Boolean): Unit = ???

  override def historicalTicksBidAsk(i: Int, list: util.List[HistoricalTickBidAsk], b: Boolean): Unit = ???

  override def historicalTicksLast(i: Int, list: util.List[HistoricalTickLast], b: Boolean): Unit = ???

  override def tickByTickAllLast(i: Int, i1: Int, l: Long, v: Double, i2: Int, tickAttribLast: TickAttribLast, s: String, s1: String): Unit = ???

  override def tickByTickBidAsk(i: Int, l: Long, v: Double, v1: Double, i1: Int, i2: Int, tickAttribBidAsk: TickAttribBidAsk): Unit = ???

  override def tickByTickMidPoint(i: Int, l: Long, v: Double): Unit = ???

  override def orderBound(l: Long, i: Int, i1: Int): Unit = ???

  override def completedOrder(contract: Contract, order: Order, orderState: OrderState): Unit = ???

  override def completedOrdersEnd(): Unit = ???

  override def replaceFAEnd(i: Int, s: String): Unit = ???
}

package serv1.client

import com.ib.client._
import com.typesafe.config.Config
import serv1.client.converters.{BarSizeConverter, ContractConverter}
import serv1.client.operations.ClientOperationHandlers
import serv1.config.ServConfig
import serv1.util.{LocalDateTimeUtil, PowerOperator}
import slick.util.Logging

import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import java.{lang, util}

object TWSClient extends DataClient with EWrapper with Logging with PowerOperator {
  var config: Config = ServConfig.config.getConfig("twsClient")
  var signal: EReaderSignal = new EJavaSignal
  var client: EClientSocket = new EClientSocket(this, signal)

  //noinspection ConvertNullInitializerToUnderscore
  @volatile
  var reader: EReader = null
  val readerMonitor = new Object
  val readerThreadState = new AtomicInteger(1)
  var thread: Thread = new Thread {
    override def run(): Unit = {
      while (readerThreadState.get() == 1) {
        if (client.isConnected) {
          logger.debug("client is connected")
          logger.debug("wait reader created")
          //noinspection LoopVariableNotUpdated
          while (reader == null) {
            readerMonitor.synchronized {
              if (reader == null) {
                readerMonitor.wait()
              }
            }
          }
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
      sequence.set(1)
      logger.debug("reset sequence")

      //call connect asynchronously
      logger.debug("start connecting")
      client.eConnect(config.getString("host"),
        config.getString("port").toInt,
        config.getString("clientId").toInt)
      logger.debug(s"Server version: ${client.serverVersion()}")
      readerMonitor.synchronized {
        reader = new EReader(client, signal)
        reader.start()
        readerMonitor.notify()
      }
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
    val durationSecond = to - from
    logger.debug(s"Duration seconds : $durationSecond")
    val duration = BarSizeConverter.getDuration(durationSecond.toInt, barSize)
    val barSizeStr = BarSizeConverter.getBarSize(barSize)
    val dateFormat = BarSizeConverter.getDateFormat(barSize)
    logger.warn(s"historicalData request: $reqN: $contract, $queryTime, $duration, $barSizeStr")
    ClientOperationHandlers.addHistoricalDataHandler(reqN, 10 ** prec, dateFormat, cont, error)
    client.reqHistoricalData(reqN, contract, queryTime, duration, barSizeStr, "TRADES", 1, dateFormat, false, null)
  }

  override def tickPrice(i: Int, i1: Int, v: Double, tickAttrib: TickAttrib): Unit =
    logger.debug("tickPrice response")

  override def tickSize(i: Int, i1: Int, i2: Int): Unit =
    logger.debug("tickSize response")

  override def tickOptionComputation(i: Int, i1: Int, i2: Int, v: Double, v1: Double, v2: Double, v3: Double, v4: Double, v5: Double, v6: Double, v7: Double): Unit =
    logger.debug("tickOptionComputation response")

  override def tickGeneric(i: Int, i1: Int, v: Double): Unit =
    logger.debug("tickGeneric response")

  override def tickString(i: Int, i1: Int, s: String): Unit =
    logger.debug("tickString response")

  override def tickEFP(i: Int, i1: Int, v: Double, s: String, v1: Double, i2: Int, s1: String, v2: Double, v3: Double): Unit =
    logger.debug("tickEFP response")

  override def orderStatus(i: Int, s: String, v: Double, v1: Double, v2: Double, i1: Int, i2: Int, v3: Double, i3: Int, s1: String, v4: Double): Unit =
    logger.debug("orderStatus response")

  override def openOrder(i: Int, contract: Contract, order: Order, orderState: OrderState): Unit =
    logger.debug("openOrder response")

  override def openOrderEnd(): Unit =
    logger.debug("openOrderEnd response")

  override def updateAccountValue(s: String, s1: String, s2: String, s3: String): Unit =
    logger.debug("updateAccountValue response")

  override def updatePortfolio(contract: Contract, v: Double, v1: Double, v2: Double, v3: Double, v4: Double, v5: Double, s: String): Unit =
    logger.debug("updatePortfolio response")

  override def updateAccountTime(s: String): Unit =
    logger.debug("updateAccountTime response")

  override def accountDownloadEnd(s: String): Unit =
    logger.debug("accountDownloadEnd response")

  override def nextValidId(i: Int): Unit = {
    logger.debug(s"nextValidId: $i")
    orderSequence.synchronized {
      logger.debug("setting in sync block")
      orderSequence.set(i)
      logger.debug("notify thread to unblock connection thread")
      orderSequence.notify()
    }
  }

  override def contractDetails(i: Int, contractDetails: ContractDetails): Unit =
    logger.debug("contractDetails response")

  override def bondContractDetails(i: Int, contractDetails: ContractDetails): Unit =
    logger.debug("bondContractDetails response")

  override def contractDetailsEnd(i: Int): Unit =
    logger.debug("contractDetailsEnd response")

  override def execDetails(i: Int, contract: Contract, execution: Execution): Unit =
    logger.debug("execDetails response")

  override def execDetailsEnd(i: Int): Unit =
    logger.debug("execDetailsEnd response")

  override def updateMktDepth(i: Int, i1: Int, i2: Int, i3: Int, v: Double, i4: Int): Unit =
    logger.debug("updateMktDepth response")

  override def updateMktDepthL2(i: Int, i1: Int, s: String, i2: Int, i3: Int, v: Double, i4: Int, b: Boolean): Unit =
    logger.debug("updateMktDepthL2 response")

  override def updateNewsBulletin(i: Int, i1: Int, s: String, s1: String): Unit =
    logger.debug("updateNewsBulletin response")

  override def managedAccounts(s: String): Unit = {
    logger.debug(s"Managed accounts: $s")
  }

  override def receiveFA(i: Int, s: String): Unit =
    logger.debug("receiveFA response")

  override def historicalData(i: Int, bar: Bar): Unit = {
    logger.debug(s"historical data: $i: ${bar.time()}")
    ClientOperationHandlers.handleHistoricalData(i, bar, last = false)
  }

  override def historicalDataEnd(i: Int, s: String, s1: String): Unit = {
    logger.debug(s"historical data end: $i: $s $s1")
    ClientOperationHandlers.handleHistoricalData(i, null, last = true)
  }


  override def scannerParameters(s: String): Unit =
    logger.debug("scannerParameters response")

  override def scannerData(i: Int, i1: Int, contractDetails: ContractDetails, s: String, s1: String, s2: String, s3: String): Unit =
    logger.debug("scannerData response")

  override def scannerDataEnd(i: Int): Unit =
    logger.debug("scannerDataEnd response")

  override def realtimeBar(i: Int, l: Long, v: Double, v1: Double, v2: Double, v3: Double, l1: Long, v4: Double, i1: Int): Unit =
    logger.debug("realtimeBar response")

  override def currentTime(l: Long): Unit =
    logger.debug("currentTime response")

  override def fundamentalData(i: Int, s: String): Unit =
    logger.debug("fundamentalData response")

  override def deltaNeutralValidation(i: Int, deltaNeutralContract: DeltaNeutralContract): Unit =
    logger.debug("deltaNeutralValidation response")

  override def tickSnapshotEnd(i: Int): Unit =
    logger.debug("tickSnapshotEnd response")

  override def marketDataType(i: Int, i1: Int): Unit =
    logger.debug("marketDataType response")

  override def commissionReport(commissionReport: CommissionReport): Unit =
    logger.debug("commissionReport response")

  override def position(s: String, contract: Contract, v: Double, v1: Double): Unit =
    logger.debug("position response")

  override def positionEnd(): Unit =
    logger.debug("positionEnd response")

  override def accountSummary(i: Int, s: String, s1: String, s2: String, s3: String): Unit =
    logger.debug("accountSummary response")

  override def accountSummaryEnd(i: Int): Unit =
    logger.debug("accountSummaryEnd response")

  override def verifyMessageAPI(s: String): Unit =
    logger.debug("verifyMessageAPI response")

  override def verifyCompleted(b: Boolean, s: String): Unit =
    logger.debug("verifyCompleted response")

  override def verifyAndAuthMessageAPI(s: String, s1: String): Unit =
    logger.debug("verifyAndAuthMessageAPI response")

  override def verifyAndAuthCompleted(b: Boolean, s: String): Unit =
    logger.debug("verifyAndAuthCompleted response")

  override def displayGroupList(i: Int, s: String): Unit =
    logger.debug("displayGroupList response")

  override def displayGroupUpdated(i: Int, s: String): Unit =
    logger.debug("displayGroupUpdated response")

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

  override def connectionClosed(): Unit =
    logger.debug("connectionClosed response")

  override def connectAck(): Unit = {
    readerThreadState.synchronized {
      logger.debug("connectAck")
      readerThreadState.notify()
    }
  }

  override def positionMulti(i: Int, s: String, s1: String, contract: Contract, v: Double, v1: Double): Unit =
    logger.debug("positionMulti response")

  override def positionMultiEnd(i: Int): Unit =
    logger.debug("positionMultiEnd response")

  override def accountUpdateMulti(i: Int, s: String, s1: String, s2: String, s3: String, s4: String): Unit =
    logger.debug("accountUpdateMulti response")

  override def accountUpdateMultiEnd(i: Int): Unit =
    logger.debug("accountUpdateMultiEnd response")

  override def securityDefinitionOptionalParameter(i: Int, s: String, i1: Int, s1: String, s2: String, set: util.Set[String], set1: util.Set[lang.Double]): Unit =
    logger.debug("securityDefinitionOptionalParameter response")

  override def securityDefinitionOptionalParameterEnd(i: Int): Unit =
    logger.debug("securityDefinitionOptionalParameterEnd response")

  override def softDollarTiers(i: Int, softDollarTiers: Array[SoftDollarTier]): Unit =
    logger.debug("softDollarTiers response")

  override def familyCodes(familyCodes: Array[FamilyCode]): Unit =
    logger.debug("familyCodes response")

  override def symbolSamples(i: Int, contractDescriptions: Array[ContractDescription]): Unit =
    logger.debug("symbolSamples response")

  override def mktDepthExchanges(depthMktDataDescriptions: Array[DepthMktDataDescription]): Unit =
    logger.debug("mktDepthExchanges response")

  override def tickNews(i: Int, l: Long, s: String, s1: String, s2: String, s3: String): Unit =
    logger.debug("tickNews response")

  override def smartComponents(i: Int, map: util.Map[Integer, util.Map.Entry[String, Character]]): Unit =
    logger.debug("smartComponents response")

  override def tickReqParams(i: Int, v: Double, s: String, i1: Int): Unit =
    logger.debug("tickReqParams response")

  override def newsProviders(newsProviders: Array[NewsProvider]): Unit =
    logger.debug("newsProviders response")

  override def newsArticle(i: Int, i1: Int, s: String): Unit =
    logger.debug("newsArticle response")

  override def historicalNews(i: Int, s: String, s1: String, s2: String, s3: String): Unit =
    logger.debug("historicalNews response")

  override def historicalNewsEnd(i: Int, b: Boolean): Unit =
    logger.debug("historicalNewsEnd response")

  override def headTimestamp(i: Int, s: String): Unit =
    logger.debug("headTimestamp response")

  override def histogramData(i: Int, list: util.List[HistogramEntry]): Unit =
    logger.debug("histogramData response")

  override def historicalDataUpdate(i: Int, bar: Bar): Unit =
    logger.debug("historicalDataUpdate response")

  override def rerouteMktDataReq(i: Int, i1: Int, s: String): Unit =
    logger.debug("rerouteMktDataReq response")

  override def rerouteMktDepthReq(i: Int, i1: Int, s: String): Unit =
    logger.debug("rerouteMktDepthReq response")

  override def marketRule(i: Int, priceIncrements: Array[PriceIncrement]): Unit =
    logger.debug("marketRule response")

  override def pnl(i: Int, v: Double, v1: Double, v2: Double): Unit =
    logger.debug("pnl response")

  override def pnlSingle(i: Int, i1: Int, v: Double, v1: Double, v2: Double, v3: Double): Unit =
    logger.debug("pnlSingle response")

  override def historicalTicks(i: Int, list: util.List[HistoricalTick], b: Boolean): Unit =
    logger.debug("historicalTicks response")

  override def historicalTicksBidAsk(i: Int, list: util.List[HistoricalTickBidAsk], b: Boolean): Unit =
    logger.debug("historicalTicksBidAsk response")

  override def historicalTicksLast(i: Int, list: util.List[HistoricalTickLast], b: Boolean): Unit =
    logger.debug("historicalTicksLast response")

  override def tickByTickAllLast(i: Int, i1: Int, l: Long, v: Double, i2: Int, tickAttribLast: TickAttribLast, s: String, s1: String): Unit =
    logger.debug("tickByTickAllLast response")

  override def tickByTickBidAsk(i: Int, l: Long, v: Double, v1: Double, i1: Int, i2: Int, tickAttribBidAsk: TickAttribBidAsk): Unit =
    logger.debug("tickByTickBidAsk response")

  override def tickByTickMidPoint(i: Int, l: Long, v: Double): Unit =
    logger.debug("tickByTickMidPoint response")

  override def orderBound(l: Long, i: Int, i1: Int): Unit =
    logger.debug("orderBound response")

  override def completedOrder(contract: Contract, order: Order, orderState: OrderState): Unit =
    logger.debug("completedOrder response")

  override def completedOrdersEnd(): Unit =
    logger.debug("completedOrdersEnd response")

  override def replaceFAEnd(i: Int, s: String): Unit =
    logger.debug("replaceFAEnd response")
}

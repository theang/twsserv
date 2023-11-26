package serv1.db

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import serv1.db.types.HistoricalDataType
import serv1.db.types.HistoricalDataType.HistoricalDataType
import serv1.model.job._
import serv1.model.ticker.BarSizes.BarSize
import serv1.model.ticker.{BarSizes, TickerError, TickerLoadType, TickerType}
import serv1.util.LocalDateTimeUtil
import spray.json._

import java.time.LocalDateTime
import java.time.format.DateTimeParseException

trait DBJsonFormats extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object LocalDateTimeJsonFormat extends RootJsonFormat[LocalDateTime] {

    override def write(obj: LocalDateTime): JsString = JsString(LocalDateTimeUtil.format(obj))

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(s) =>
        try {
          LocalDateTimeUtil.parse(s)
        } catch {
          case exc: DateTimeParseException =>
            throw DeserializationException(s"Cant parse date $s", exc)
        }
      case _ => throw DeserializationException("Date time in ISO format expected")
    }
  }


  implicit val tickerTypeFormat: RootJsonFormat[TickerType] = jsonFormat11(TickerType)

  implicit object BarSizeFormat extends RootJsonFormat[BarSize] {
    override def write(obj: BarSize): JsString = JsString(obj.toString)

    override def read(json: JsValue): BarSize = json match {
      case JsString(s) =>
        try {
          BarSizes.withName(s)
        } catch {
          case exc: NoSuchElementException => throw DeserializationException("Need BarSize", exc)
        }
      case _ => throw DeserializationException(s"Need BarSize")
    }
  }

  implicit object HistoricalDataTypeFormat extends RootJsonFormat[HistoricalDataType] {
    override def write(obj: HistoricalDataType): JsString = JsString(obj.toString)

    override def read(json: JsValue): HistoricalDataType = json match {
      case JsString(s) =>
        try {
          HistoricalDataType.withName(s)
        } catch {
          case exc: NoSuchElementException => throw DeserializationException("Need HistoricalDataType", exc)
        }
      case _ => throw DeserializationException("Need HistoricalDataType")
    }
  }

  implicit object TickerLoadTypeFormat extends RootJsonFormat[TickerLoadType] {
    override def write(obj: TickerLoadType): JsObject = JsObject(
      "tickerType" -> obj.tickerType.toJson,
      "barSize" -> obj.barSize.toJson,
      "historicalDataType" -> obj.historicalDataType.toJson
    )

    override def read(json: JsValue): TickerLoadType = {
      List("tickerType", "barSize", "historicalDataType")
        .map(json.asJsObject.fields.get) match {
        case Seq(tickerType, barSize, historicalDataType) =>
          TickerLoadType(tickerType.get.convertTo[TickerType],
            barSize.get.convertTo[BarSizes.BarSize],
            historicalDataType.map(_.convertTo[HistoricalDataType]).getOrElse(HistoricalDataType.TRADES)
          )
      }
    }
  }


  implicit val tickerErrorFormat: RootJsonFormat[TickerError] = jsonFormat2(TickerError)

  implicit object JobStatusJsonFormat extends RootJsonFormat[JobStatuses.JobStatus] {
    override def write(obj: JobStatuses.JobStatus): JsString = JsString(obj.toString)

    override def read(json: JsValue): JobStatuses.JobStatus = json match {
      case JsString(s) =>
        try {
          JobStatuses.withName(s)
        } catch {
          case exc: NoSuchElementException => throw DeserializationException("Need JobStatus", exc)
        }
      case _ => throw DeserializationException("Need JobStatus")
    }
  }


  implicit object TickerJobStateFormat extends RootJsonFormat[TickerJobState] {
    override def write(obj: TickerJobState): JsObject = JsObject(
      "status" -> obj.status.toJson,
      "tickers" -> obj.tickers.toJson,
      "errors" -> obj.errors.toJson,
      "loadedTickers" -> obj.loadedTickers.toJson,
      "ignoredTickers" -> obj.ignoredTickers.toJson,
      "from" -> obj.from.toJson,
      "to" -> obj.to.toJson,
      "overwrite" -> obj.overwrite.toJson
    )

    override def read(json: JsValue): TickerJobState = {
      List("status", "tickers", "errors", "loadedTickers", "ignoredTickers", "from", "to", "overwrite")
        .map(json.asJsObject.fields.get) match {
        case Seq(status, tickers, errors, loadedTickers, ignoredTickers, from, to, overwrite) =>
          TickerJobState(status.get.convertTo[JobStatuses.JobStatus],
            tickers.get.convertTo[List[TickerLoadType]],
            loadedTickers.fold(List[TickerLoadType]())(_.convertTo[List[TickerLoadType]]),
            ignoredTickers.fold(List[TickerLoadType]())(_.convertTo[List[TickerLoadType]]),
            errors.fold(List[TickerError]())(_.convertTo[List[TickerError]]),
            from.get.convertTo[LocalDateTime],
            to.get.convertTo[LocalDateTime],
            overwrite.exists(_.convertTo[Boolean])
          )
      }
    }
  }

  implicit object TickLoadingJobStateFormat extends RootJsonFormat[TickLoadingJobState] {
    override def write(obj: TickLoadingJobState): JsObject = JsObject(
      "status" -> obj.status.toJson,
      "tickers" -> obj.tickers.toJson,
      "errors" -> obj.errors.toJson
    )

    override def read(json: JsValue): TickLoadingJobState = {
      List("status", "tickers", "errors")
        .map(json.asJsObject.fields.get) match {
        case Seq(status, tickers, errors) =>
          TickLoadingJobState(status.get.convertTo[JobStatuses.JobStatus],
            tickers.get.convertTo[List[TickerLoadType]],
            errors.fold(List[TickerError]())(_.convertTo[List[TickerError]])
          )
      }
    }
  }

  implicit object EarningsLoadingJobStateFormat extends RootJsonFormat[EarningsLoadingJobState] {
    override def write(obj: EarningsLoadingJobState): JsObject = JsObject(
      "status" -> obj.status.toJson,
      "from" -> obj.from.toJson,
      "to" -> obj.to.toJson,
      "current" -> obj.current.toJson
    )

    override def read(json: JsValue): EarningsLoadingJobState = {
      List("status", "from", "to", "current")
        .map(json.asJsObject.fields.get) match {
        case Seq(status, from, to, current) =>
          EarningsLoadingJobState(status.get.convertTo[JobStatuses.JobStatus],
            from.get.convertTo[LocalDateTime],
            to.get.convertTo[LocalDateTime],
            current.get.convertTo[LocalDateTime]
          )
      }
    }
  }

  implicit object JobStateFormat extends RootJsonFormat[JobState] {
    override def write(obj: JobState): JsObject = {
      val (b: JobState, jsVal: JsValue) = obj match {
        case b: TickerJobState => (b, b.toJson)
        case b: TickLoadingJobState => (b, b.toJson)
        case b: EarningsLoadingJobState => (b, b.toJson)
      }
      JsObject(
        "class" -> b.getClass.getSimpleName.toJson,
        "data" -> jsVal
      )
    }

    override def read(json: JsValue): JobState = {
      val cl = json.asJsObject().getFields("class").head.convertTo[String]
      val dataJs: JsValue = json.asJsObject().getFields("data").head
      cl match {
        case "TickerJobState" =>
          dataJs.convertTo[TickerJobState]
        case "TickLoadingJobState" =>
          dataJs.convertTo[TickLoadingJobState]
        case "EarningsLoadingJobState" =>
          dataJs.convertTo[EarningsLoadingJobState]
      }
    }
  }
}

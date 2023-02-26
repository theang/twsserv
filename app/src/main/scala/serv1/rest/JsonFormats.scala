package serv1.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.DateTime
import serv1.job.{JobState, TickerJobState}
import serv1.model.HistoricalData
import serv1.model.job.JobStatuses
import serv1.model.ticker.BarSizes.BarSize
import serv1.model.ticker.{BarSizes, TickerError, TickerLoadType, TickerType}
import serv1.rest.historical.HistoricalDataActor.{HistoricalDataResponse, HistoricalDataValues}
import serv1.rest.loaddata.LoadDataActor.{LoadDataRequest, LoadDataResponse, LoadPeriod}
import serv1.rest.schedule.ScheduleActor._
import serv1.rest.ticker.TickerJobControlActor.{AddTickersTrackingRequest, GetStatusRequest, RemoveTickersTrackingRequest, TickersTrackingResponse}
import serv1.util.LocalDateTimeUtil
import spray.json._

import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

trait JsonFormats extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object DateTimeJsonFormat extends RootJsonFormat[DateTime] {

    override def write(obj: DateTime): JsString = JsString(obj.toIsoDateString())

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => DateTime.fromIsoDateTimeString(s).getOrElse(throw DeserializationException(s"Cant parse date $s"))
      case _ => throw DeserializationException("Date time in ISO format expected")
    }
  }

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

  implicit val tickerTypeFormat: RootJsonFormat[TickerType] = jsonFormat4(TickerType)

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

  implicit val tickerLoadTypeFormat: RootJsonFormat[TickerLoadType] = jsonFormat2(TickerLoadType)
  implicit val tickerErrorFormat: RootJsonFormat[TickerError] = jsonFormat2(TickerError)
  implicit val loadPeriodFormat: RootJsonFormat[LoadPeriod] = jsonFormat2(LoadPeriod)
  implicit val loadDataRequestFormat: RootJsonFormat[LoadDataRequest] = jsonFormat2(LoadDataRequest)

  implicit val historicalDataFormat: RootJsonFormat[HistoricalData] = jsonFormat6(HistoricalData)

  implicit object UUIDJsonFormat extends RootJsonFormat[UUID] {
    override def write(obj: UUID): JsString = JsString(obj.toString)

    override def read(json: JsValue): UUID = json match {
      case JsString(s) => UUID.fromString(s)
      case _ => throw DeserializationException("Need UUID")
    }
  }

  implicit val loadDataResponseFormat: RootJsonFormat[LoadDataResponse] = jsonFormat1(LoadDataResponse) // contains List[Item]

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
      "from" -> obj.from.toJson,
      "to" -> obj.to.toJson
    )

    override def read(json: JsValue): TickerJobState = {
      json.asJsObject.getFields("status", "tickers", "errors", "loadedTickers", "from", "to") match {
        case Seq(status, tickers, errors, loadedTickers, from, to) =>
          TickerJobState(status.convertTo[JobStatuses.JobStatus],
            tickers.convertTo[List[TickerLoadType]],
            loadedTickers.convertTo[List[TickerLoadType]],
            errors.convertTo[List[TickerError]],
            from.convertTo[LocalDateTime],
            to.convertTo[LocalDateTime]
          )
      }
    }
  }

  implicit object JobStateFormat extends RootJsonFormat[JobState] {
    override def write(obj: JobState): JsObject = {
      val (b: JobState, jsVal: JsValue) = obj match {
        case b: TickerJobState => (b, b.toJson)
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
      }
    }
  }

  implicit val runScheduledTaskRequest: RootJsonFormat[RunScheduledTaskRequest] = jsonFormat3(RunScheduledTaskRequest)
  implicit val createScheduledTaskRequestFormat: RootJsonFormat[CreateScheduledTaskRequest] = jsonFormat2(CreateScheduledTaskRequest)
  implicit val renameScheduledTaskRequestFormat: RootJsonFormat[RenameTaskRequest] = jsonFormat2(RenameTaskRequest)
  implicit val changeScheduleOfScheduledTaskRequestFormat: RootJsonFormat[ChangeScheduleRequest] = jsonFormat2(ChangeScheduleRequest)
  implicit val createScheduledTaskResponseFormat: RootJsonFormat[ScheduledTaskResponse] = jsonFormat4(ScheduledTaskResponse)

  implicit val addTickersTrackingRequestFormat: RootJsonFormat[AddTickersTrackingRequest] = jsonFormat2(AddTickersTrackingRequest)
  implicit val removeTickersTrackingRequestFormat: RootJsonFormat[RemoveTickersTrackingRequest] = jsonFormat2(RemoveTickersTrackingRequest)
  implicit val getStatusRequestFormat: RootJsonFormat[GetStatusRequest] = jsonFormat1(GetStatusRequest)
  implicit val tickersTrackingResponseFormat: RootJsonFormat[TickersTrackingResponse] = jsonFormat2(TickersTrackingResponse)

  implicit val historicalDataValuesFormat: RootJsonFormat[HistoricalDataValues] = jsonFormat2(HistoricalDataValues)
  implicit val historicalDataResponseFormat: RootJsonFormat[HistoricalDataResponse] = jsonFormat1(HistoricalDataResponse)
}

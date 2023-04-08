package serv1.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.DateTime
import serv1.db.DBJsonFormats
import serv1.model.HistoricalData
import serv1.rest.historical.HistoricalDataActor.{HistoricalDataResponse, HistoricalDataValues}
import serv1.rest.loaddata.LoadDataActor._
import serv1.rest.schedule.ScheduleActor._
import serv1.rest.ticker.TickerJobControlActor.{AddTickersTrackingRequest, GetStatusRequest, RemoveTickersTrackingRequest, TickersTrackingResponse}
import spray.json._

import java.util.UUID

trait JsonFormats extends SprayJsonSupport with DefaultJsonProtocol with DBJsonFormats {

  implicit object DateTimeJsonFormat extends RootJsonFormat[DateTime] {

    override def write(obj: DateTime): JsString = JsString(obj.toIsoDateString())

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => DateTime.fromIsoDateTimeString(s).getOrElse(throw DeserializationException(s"Cant parse date $s"))
      case _ => throw DeserializationException("Date time in ISO format expected")
    }
  }

  implicit val loadPeriodFormat: RootJsonFormat[LoadPeriod] = jsonFormat2(LoadPeriod)
  implicit val loadDataRequestFormat: RootJsonFormat[LoadDataRequest] = jsonFormat2(LoadDataRequest)
  implicit val reloadDataRequestFormat: RootJsonFormat[ReloadDataRequest] = jsonFormat1(ReloadDataRequest)

  implicit val historicalDataFormat: RootJsonFormat[HistoricalData] = jsonFormat6(HistoricalData)

  implicit object UUIDJsonFormat extends RootJsonFormat[UUID] {
    override def write(obj: UUID): JsString = JsString(obj.toString)

    override def read(json: JsValue): UUID = json match {
      case JsString(s) => UUID.fromString(s)
      case _ => throw DeserializationException("Need UUID")
    }
  }

  implicit val loadDataResponseFormat: RootJsonFormat[LoadDataResponse] = jsonFormat1(LoadDataResponse) // contains List[Item]
  implicit val loadDataResponsesFormat: RootJsonFormat[LoadDataResponses] = jsonFormat1(LoadDataResponses)

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

  implicit val scheduledTasksResponseFormat: RootJsonFormat[ScheduledTasksResponse] = jsonFormat1(ScheduledTasksResponse)
}

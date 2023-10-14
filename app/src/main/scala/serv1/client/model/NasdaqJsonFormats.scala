package serv1.client.model

import serv1.util.LocalDateTimeUtil
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

import java.text.{DecimalFormat, DecimalFormatSymbols, ParseException}
import java.time.LocalDate
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.util.Locale

trait NasdaqJsonFormats extends DefaultJsonProtocol {

  val formatterMDY: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

  implicit object EarningsFormat extends RootJsonFormat[Earnings] {
    override def write(obj: Earnings): JsValue = {
      JsString("Not implemented")
    }

    val decimalFormat = new DecimalFormat("'$'#,##0.00;('$'#,##0.00)", new DecimalFormatSymbols(Locale.US))

    def convertMoney(v: String): Option[Double] = {
      if (v.isBlank || v == "N/A") {
        None
      } else {
        try {
          Some(decimalFormat.parse(v).doubleValue())
        } catch {
          case _: ParseException => None
        }
      }
    }

    def convertDate(v: String): Option[Long] = {
      if (v.isBlank || v == "N/A") {
        None
      } else {
        try {
          Some(LocalDateTimeUtil.toEpoch(LocalDate.parse(v, formatterMDY).atStartOfDay()))
        } catch {
          case _: DateTimeParseException => None
        }
      }
    }

    override def read(json: JsValue): Earnings = {
      List("symbol", "eps", "marketCap", "fiscalQuarterEnding", "epsForecast", "lastYearRptDt", "lastYearEPS").map(json.asJsObject.fields.get) match {
        case Seq(symbol, eps, marketCap, fiscalQuarterEnding, epsForecast, lastYearRptDt, lastYearEPS) =>
          Earnings(symbol.get.convertTo[String], "",
            eps.isEmpty,
            fiscalQuarterEnding.get.convertTo[String],
            eps.flatMap(v => convertMoney(v.convertTo[String])),
            epsForecast.flatMap(v => convertMoney(v.convertTo[String])),
            marketCap.flatMap(v => convertMoney(v.convertTo[String])),
            lastYearEPS.flatMap(v => convertMoney(v.convertTo[String])),
            lastYearRptDt.flatMap(v => convertDate(v.convertTo[String]))
          )
      }
    }
  }
}

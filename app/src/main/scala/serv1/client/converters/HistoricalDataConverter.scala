package serv1.client.converters

import com.ib.client.Bar
import serv1.model.HistoricalData
import serv1.util.LocalDateTimeUtil

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object HistoricalDataConverter {
  val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  def fromPrecAndDateFormat (prec: Int, dateFormat: Int): Bar=>HistoricalData = {
    (b: Bar) =>
      HistoricalData(
        dateFormat match {
          case 1 => LocalDateTimeUtil.toEpoch(LocalDateTime.parse(b.time(), formatter))
          case 2 => b.time.toLong
        },
        (b.high() * prec).toLong, (b.low() * prec).toLong,
        (b.open() * prec).toLong, (b.close() * prec).toLong,
        b.volume())
  }
}

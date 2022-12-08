package serv1.util

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

object LocalDateTimeUtil {
  private val ISODateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  def parse(s: String): LocalDateTime = LocalDateTime.parse(s, ISODateTimeFormatter)
  def format(dt: LocalDateTime): String = dt.format(ISODateTimeFormatter)
  def fromEpoch(epoch: Long): LocalDateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC)
  def toEpoch(dt: LocalDateTime): Long = dt.toEpochSecond(ZoneOffset.UTC)
}

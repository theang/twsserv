package serv1.util

import java.time.format.DateTimeFormatter
import java.time._

object LocalDateTimeUtil {
  private val ISODateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  def parse(s: String): LocalDateTime = LocalDateTime.parse(s, ISODateTimeFormatter)

  def format(dt: LocalDateTime): String = dt.format(ISODateTimeFormatter)

  def fromEpoch(epoch: Long): LocalDateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC)

  def toEpoch(dt: LocalDateTime): Long = dt.toEpochSecond(ZoneOffset.UTC)

  def getYearDays(year: Int): Int = Year.of(year).length()

  /// Sunday = 0
  def getDayOfWeekStarting(year: Int): Int = Year.of(year).atDay(1).getDayOfWeek.getValue % 7

  /// Sunday = 0
  def getDayOfWeek(year: Int, month: Int, day: Int): Int = LocalDate.of(year, month, day).getDayOfWeek.getValue % 7

  def getMonthDays(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

  def getCurrentDateTimeUTC: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

  def convertFromUTC(dateTime: LocalDateTime, zoneId: ZoneId): LocalDateTime = dateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId).toLocalDateTime

  def convertToUTC(dateTime: LocalDateTime, zoneId: ZoneId): LocalDateTime = dateTime.atZone(zoneId).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime
}

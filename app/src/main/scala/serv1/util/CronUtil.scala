package serv1.util

import java.time.{LocalDateTime, Year, YearMonth}
import scala.util.control.Breaks.{break, breakable}

object CronUtil {

  case class RestrictedRange(restricted: Boolean, from: Int, to: Int, allowed: List[Int])

  case class ParsedCronExpr(minuteRange: RestrictedRange,
                            hourRange: RestrictedRange,
                            dayRange: RestrictedRange,
                            monthRange: RestrictedRange,
                            dowRange: RestrictedRange)

  def getDivisor(atom: String): Int = {
    if (atom.contains("/")) {
      atom.split("/").last.toInt
    } else {
      1
    }
  }

  def leaveOnlyDivisible(values: List[Int], divisor: Int): List[Int] =
    if (divisor > 1) values.filter(vl => (vl % divisor) == 0) else values

  def parseOneAtom(atom: String, gFrom: Int, gTo: Int): List[Int] = {
    val atomSuffixRemoved = atom.split("/").head
    val list = if (atomSuffixRemoved.contains("-")) {
      atomSuffixRemoved.split("-").map(_.toInt) match {
        case Array(from, to) => List.range(from, to + 1)
        case _ => List.empty
      }
    } else {
      List(atomSuffixRemoved.toInt)
    }
    leaveOnlyDivisible(list, getDivisor(atom))
  }

  def parseOneInternal(pattern: String, gFrom: Int, gTo: Int): List[Int] = {
    if (pattern.startsWith("*")) {
      leaveOnlyDivisible(List.range(gFrom, gTo + 1), getDivisor(pattern))
    } else {
      pattern.split(",").flatMap {
        str: String =>
          parseOneAtom(str, gFrom, gTo)
      }.toSet.toList.sorted
    }
  }

  def parseOne(pattern: String, gFrom: Int, gTo: Int): RestrictedRange = {
    if (pattern == "*") {
      RestrictedRange(restricted = false, gFrom, gTo, List.range(gFrom, gTo + 1))
    }
    val res: List[Int] = parseOneInternal(pattern, gFrom, gTo)
    if (res.isEmpty) {
      throw new IllegalArgumentException(s"$pattern results in empty match, it will be never met")
    }
    RestrictedRange(restricted = true, gFrom, gTo, res)
  }

  def parseCronExpr(pattern: String): ParsedCronExpr = {
    pattern.split(" +") match {
      case Array(minute, hour, day, month, dow) =>
        val MR = parseOne(minute, 0, 59)
        val HR = parseOne(hour, 0, 23)
        val dR = parseOne(day, 1, 31)
        val mR = parseOne(month, 1, 12)
        val dowR = parseOne(dow, 0, 6)
        ParsedCronExpr(MR, HR, dR, mR, dowR)
      case _ =>
        throw new IllegalArgumentException("pattern should be in format: minute hour day month dow")
    }
  }

  // finds next run (includes currentTime)
  // if currentTime matches pattern it will be returned as nextRun
  def findNextRun(currentTime: Long, pattern: String): Long = {
    var res: Long = 0
    var curDt: LocalDateTime = LocalDateTimeUtil.fromEpoch(currentTime)
    val parsed = parseCronExpr(pattern)

    var found: Boolean = false

    while ((!found) && (curDt.getYear < 2999)) {
      breakable {
        val month = curDt.getMonth.getValue
        val monthRange = parsed.monthRange.allowed
        if (!monthRange.contains(month)) {
          val nextMonthThisYear = monthRange.find(_ > month)
          val nextMonth = nextMonthThisYear
            .getOrElse(monthRange.head)
          if (nextMonthThisYear.isEmpty) {
            // add year and start from the beginning
            curDt = Year.of(curDt.getYear).plusYears(1).atMonth(nextMonth).atDay(1).atStartOfDay()
            break()
          }
          curDt = Year.from(curDt).atMonth(nextMonth).atDay(1).atStartOfDay()
          break()
        }
        val day = curDt.getDayOfMonth
        // sun = 0, ... , sat = 6
        val dayOfWeek = curDt.getDayOfWeek.getValue % 7
        val dRestricted = parsed.dayRange.restricted
        val dowRestricted = parsed.dowRange.restricted
        val dayRange = parsed.dayRange.allowed
        if (dRestricted && !dowRestricted && !dayRange.contains(day)) {
          val nextDayThisMonth = dayRange.find(_ > day)
          val nextDay = nextDayThisMonth.getOrElse(dayRange.head)
          if (nextDayThisMonth.isEmpty || nextDay > YearMonth.from(curDt).lengthOfMonth()) {
            curDt = YearMonth.from(curDt).plusMonths(1).atDay(1).atStartOfDay()
            break()
          }
          curDt = YearMonth.from(curDt).atDay(nextDay).atStartOfDay()
          break()
        }
        val dowRange = parsed.dowRange.allowed
        if (!dRestricted && dowRestricted && !dowRange.contains(dayOfWeek)) {
          val nextDowThisWeek = dowRange.find(_ > dayOfWeek)
          val nextDow = nextDowThisWeek.getOrElse(dowRange.head)
          val daysTillNextDow = if (nextDowThisWeek.isDefined) nextDow - dayOfWeek else 7 + nextDow - dayOfWeek
          if (day + daysTillNextDow > YearMonth.from(curDt).lengthOfMonth()) {
            curDt = YearMonth.from(curDt).plusMonths(1).atDay(1).atStartOfDay()
            break()
          }
          curDt = curDt.plusDays(daysTillNextDow)
          break()
        }
        val hourRange = parsed.hourRange.allowed
        val hourRestricted = parsed.hourRange.restricted
        val hour = curDt.getHour
        if (hourRestricted && !hourRange.contains(hour)) {
          val nextHourThisDay = hourRange.find(_ > hour)
          val nextHour = nextHourThisDay.getOrElse(hourRange.head)
          if (nextHourThisDay.isEmpty) {
            curDt = curDt.toLocalDate.plusDays(1).atTime(nextHour, 0)
            break()
          }
          curDt = curDt.toLocalDate.atTime(nextHour, 0)
          break()
        }
        val minuteRange = parsed.minuteRange.allowed
        val minuteRestricted = parsed.minuteRange.restricted
        val minute = curDt.getMinute
        if (minuteRestricted && !minuteRange.contains(minute)) {
          val nextMinuteThisHour = minuteRange.find(_ > minute)
          val nextMinute = nextMinuteThisHour.getOrElse(minuteRange.head)
          if (nextMinuteThisHour.isEmpty) {
            curDt = curDt.toLocalDate.atTime(curDt.getHour, nextMinute).plusHours(1)
            break()
          }
          curDt = curDt.toLocalDate.atTime(curDt.getHour, nextMinute)
          break()
        }
        found = true
      }
    }

    if (parsed.monthRange.allowed.contains(curDt.getMonth.getValue)
      && parsed.dowRange.allowed.contains(curDt.getDayOfWeek.getValue % 7)
      && parsed.hourRange.allowed.contains(curDt.getHour)
      && parsed.minuteRange.allowed.contains(curDt.getMinute)) {
      res = LocalDateTimeUtil.toEpoch(curDt)
    } else {
      throw new CronException("Couldn't calculate nextRun")
    }
    res
  }
}


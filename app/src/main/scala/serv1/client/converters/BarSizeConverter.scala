package serv1.client.converters

import serv1.model.ticker.BarSizes

object BarSizeConverter {
  val min = 60
  val hr: Int = 60 * min
  val day: Int = hr * 24
  val week: Int = day * 7
  val bigMonth: Int = day * 31
  val commonYear: Int = day * 365

  val DATETIME_FORMAT = 1
  val EPOCH_FORMAT = 2

  def getBarSize(seconds: Int): String = {
    if (seconds > week) {
      "1 month"
    } else if (seconds > day) {
      "1 week"
    } else if (seconds > 8 * hr) {
      "1 day"
    } else if (seconds > hr) {
      s"${seconds / hr} hours"
    } else if (seconds == hr) {
      "1 hour"
    } else if (seconds > 60) {
      s"${seconds / 60} mins"
    } else if (seconds == 60) {
      "1 min"
    } else {
      s"$seconds secs"
    }
  }

  def divideCeil(dividend: Int, divisor: Int): Int =
    dividend / divisor + (if (dividend % divisor > 0) 1 else 0)

  def getDuration(seconds: Int, barSeconds: Int): String = {
    if (barSeconds > week) {
      s"${divideCeil(seconds, bigMonth)} M"
    } else if (barSeconds > day) {
      s"${divideCeil(seconds, week)} W"
    } else if ((barSeconds > 8 * hr) || (seconds > day)) {
      val days = divideCeil(seconds, day)
      if (days > 365) { // requests from more than 365 days needs to be in years
        s"${divideCeil(days, 365)} Y"
      } else {
        s"$days D"
      }
    } else {
      s"$seconds S"
    }
  }

  def getBarSizeSeconds(barSize: BarSizes.BarSize): Int = {
    barSize match {
      case BarSizes.MIN1 => min
      case BarSizes.MIN5 => 5 * min
      case BarSizes.HOUR => hr
      case BarSizes.MIN15 => 15 * min
      case BarSizes.DAY => day
    }
  }

  def getDateFormat(seconds: Int): Int = {
    if (seconds >= day) {
      DATETIME_FORMAT
    } else {
      EPOCH_FORMAT
    }
  }
}

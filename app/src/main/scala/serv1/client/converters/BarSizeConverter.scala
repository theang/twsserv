package serv1.client.converters

import serv1.model.ticker.BarSizes

object BarSizeConverter {
  val min = 60
  val hr = 60 * min
  val day = hr * 24
  val week = day * 7

  val DATETIME_FORMAT = 1
  val EPOCH_FORMAT = 2

  def getBarSize(seconds: Int): String = {
    if (seconds > week) {
      "1 month"
    } else if (seconds > day) {
      "1 week"
    } else if (seconds > 8*hr) {
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
      s"${seconds} secs"
    }
  }

  def getBarSizeSeconds(barSize: BarSizes.BarSize): Int = {
    barSize match {
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

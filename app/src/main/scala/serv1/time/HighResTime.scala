package serv1.time

import slick.util.Logging

import java.time.Instant

object HighResTime extends Logging {
  val milliSecond: Long = 1_000_000
  val second: Long = 1_000_000_000
  val tenSeconds: Long = second * 10
  val lastEpochNanoSecond: ThreadLocal[Long] = ThreadLocal.withInitial(() => currentNanosSlow)
  val lastNanoTime: ThreadLocal[Long] = ThreadLocal.withInitial(() => System.nanoTime())

  def currentNanosSlow: Long = {
    val now = Instant.now()
    now.getEpochSecond * second + now.getNano
  }

  def updateLastTime(): Unit = {
    lastEpochNanoSecond.set(currentNanosSlow)
    lastNanoTime.set(System.nanoTime())
  }

  def currentNanos: Long = {
    val lastEpochNanoSecondVal = lastEpochNanoSecond.get()
    val lastNanoTimeVal = lastNanoTime.get()
    val currentNanoTime: Long = System.nanoTime()
    if (lastNanoTimeVal + tenSeconds < currentNanoTime) {
      updateLastTime()
    }
    val result = lastEpochNanoSecondVal + (System.nanoTime() - lastNanoTimeVal)
    result
  }
}

package serv1.time

import slick.util.Logging

import java.time.Instant
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

object HighResTime extends Logging {
  val second: Long = 1_000_000_000
  var lastEpochNanoSecond: Long = currentNanosSlow
  var lastNanoTime: Long = System.nanoTime()
  val readerCanRead = new AtomicBoolean
  val readerReads = new AtomicInteger(0)
  val writeMonitor = new Object

  def currentNanosSlow: Long = {
    val now = Instant.now()
    now.getEpochSecond * 1000_000_000 + now.getNano
  }

  def updateLastTime(): Unit = {
    if (readerReads.get() > 0) {
      readerReads.synchronized {
        while (readerReads.get() > 0) {
          try {
            readerReads.wait()
          } catch {
            case exc: InterruptedException =>
              logger.warn("Interrupted", exc)
              return
          }
        }
        readerCanRead.set(false)
      }
    }
    if (lastNanoTime + second < System.nanoTime()) {
      writeMonitor.synchronized {
        if (lastNanoTime + second < System.nanoTime()) {
          lastEpochNanoSecond = currentNanosSlow
          lastNanoTime = System.nanoTime()
        }
      }
    }
    readerCanRead.synchronized {
      readerCanRead.set(true)
      readerCanRead.notifyAll()
    }
  }

  def currentNanos: Long = {
    val currentNanoTime: Long = System.nanoTime()
    if (lastNanoTime + second < currentNanoTime) {
      updateLastTime()
    }
    if (!readerCanRead.get()) {
      readerCanRead.synchronized {
        while (!readerCanRead.get()) {
          try {
            readerCanRead.wait()
          } catch {
            case exc: InterruptedException =>
              logger.warn("Interrupted", exc)
              return lastEpochNanoSecond + System.nanoTime() - lastNanoTime
          }
        }
      }
      readerReads.incrementAndGet()
    }
    val result = lastEpochNanoSecond + System.nanoTime() - lastNanoTime
    readerReads.synchronized {
      readerReads.decrementAndGet()
      readerReads.notifyAll()
    }
    result
  }
}

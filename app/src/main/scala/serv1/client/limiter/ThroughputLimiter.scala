package serv1.client.limiter

import serv1.time.HighResTime

import scala.collection.mutable

class ThroughputLimiter(intervalMs: Int, limit: Int) {
  var queue: mutable.PriorityQueue[Long] = mutable.PriorityQueue[Long]()(Ordering.by(-_))

  def currentMillis: Long = HighResTime.currentNanos / HighResTime.milliSecond

  def removeObsolete(): Unit = {
    val msTime = currentMillis
    while (queue.nonEmpty && queue.head < msTime - intervalMs) {
      queue.dequeue()
    }
  }

  def available: Int = {
    removeObsolete()
    Math.max(limit - queue.size, 0)
  }

  def append(): Unit = {
    if (available == 0) {
      throw new LimiterException(s"No available requests", null)
    }
    queue.enqueue(currentMillis)
  }

  def appendMany(requests: Int): Unit = {
    if (available < requests) {
      throw new LimiterException(s"No available requests(2)", null)
    }
    val list = List.fill(requests)(currentMillis)
    queue.enqueue(list: _*)
  }
}

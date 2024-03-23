package serv1.client.limiter

import java.util.concurrent.atomic.AtomicInteger

class SimultaneousLimiter(limit: Int) {
  val currentRequests: AtomicInteger = new AtomicInteger()

  def append: Int = {
    if (available == 0) {
      throw new LimiterException(s"No available requests", null)
    }
    currentRequests.incrementAndGet()
  }

  def appendMany(requests: Int): Int = {
    if (available < requests) {
      throw new LimiterException(s"No available requests(2)", null)
    }
    currentRequests.addAndGet(requests)
  }

  def available: Int = {
    Math.max(limit - currentRequests.get(), 0)
  }

  def current: Int = {
    currentRequests.get()
  }

  def detach: Int = {
    currentRequests.decrementAndGet()
  }
}

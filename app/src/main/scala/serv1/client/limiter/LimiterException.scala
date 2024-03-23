package serv1.client.limiter

class LimiterException(message: String, cause: Throwable)
  extends RuntimeException(LimiterException.defaultMessage(message, cause), cause)

object LimiterException {
  def defaultMessage(message: String, cause: Throwable): String = {
    if (message != null) {
      message
    } else if (cause != null) {
      cause.getMessage
    } else {
      null
    }
  }
}
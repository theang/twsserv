package serv1.util

class CronException(message: String = null, cause: Throwable = null)
  extends RuntimeException(CronException.defaultMessage(message, cause), cause)

object CronException {
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

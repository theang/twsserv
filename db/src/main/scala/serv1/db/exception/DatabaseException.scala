package serv1.db.exception

class DatabaseException(message: String = null, cause: Throwable = null)
  extends RuntimeException(DatabaseException.defaultMessage(message, cause), cause)

object DatabaseException {
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

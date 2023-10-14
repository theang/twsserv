package serv1.client.exception

class ClientException(message: String = null, cause: Throwable = null)
  extends RuntimeException(ClientException.defaultMessage(message, cause), cause)

object ClientException {
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
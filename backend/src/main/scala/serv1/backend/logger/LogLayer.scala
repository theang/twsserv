package serv1.backend.logger

import zio.ZLayer
import zio.logging.backend._

object LogLayer {
  val layer: ZLayer[Any, Nothing, Unit] = SLF4J.slf4j
}

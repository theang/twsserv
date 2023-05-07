package serv1.backend.config

import zio.Config
import zio.config.magnolia._

case class HttpServerConfig(port: Int, host: String, nThreads: Int)

object HttpServerConfig {
  val config: Config[HttpServerConfig] =
    deriveConfig[HttpServerConfig].nested("HttpServerConfig")
}

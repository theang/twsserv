package serv1.backend.config

import zio.Config
import zio.config.magnolia._
import zio.http.Header.{AccessControlAllowOrigin, Origin}
import zio.http.internal.middlewares.Cors.CorsConfig

case class HttpServerConfig(port: Int, host: String, nThreads: Int, allowedOrigin: String)

object HttpServerConfig {
  val config: Config[HttpServerConfig] =
    deriveConfig[HttpServerConfig].nested("HttpServerConfig")

  def corsConfig(config: HttpServerConfig): CorsConfig =
    CorsConfig(
      allowedOrigin = {
        case origin@Origin.Value(_, host, _) if host == config.allowedOrigin => Some(AccessControlAllowOrigin.Specific(origin))
        case _ => None
      }
    )
}

package serv1.backend

import io.getquill.jdbczio.Quill
import io.getquill.{CompositeNamingStrategy3, Escape, SnakeCase, UpperCase}
import serv1.backend.app.{HealthCheck, HistoricalData, TickerType}
import serv1.backend.config.HttpServerConfig
import serv1.backend.db.{DataSource, HistoricalDataRepo, TickerTypeRepo}
import zio._
import zio.config.typesafe.TypesafeConfigProvider
import zio.http._
import zio.logging.backend.SLF4J

object BackendApp extends ZIOAppDefault {
  type BackendEnv = TickerTypeRepo with HistoricalDataRepo

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(
      TypesafeConfigProvider.fromResourcePath()
    ) >>> Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val backendApp: Http[BackendEnv, Throwable, Request, Response] =
    HealthCheck() ++ HistoricalData() ++ TickerType()

  val backendMapError: Http[BackendEnv, Response, Request, Response] =
    backendApp.mapError { error =>
      Response(status = Status.InternalServerError, body = Body.fromString(s"Error: $error"))
    }

  val httpServerConfig: ZLayer[Any, Config.Error, HttpServerConfig] =
    ZLayer
      .fromZIO(
        ZIO.config[HttpServerConfig](HttpServerConfig.config)
      )

  def server(httpServerConfig: HttpServerConfig): ZIO[Any, Throwable, Nothing] = {
    Server.serve[BackendEnv](backendMapError)
      .provide(Server.defaultWith(_.binding(httpServerConfig.host, httpServerConfig.port)),
        Quill.Postgres.fromNamingStrategy[CompositeNamingStrategy3[SnakeCase, UpperCase, Escape]](CompositeNamingStrategy3(SnakeCase, UpperCase, Escape)),
        Quill.DataSource.fromPrefix("postgres"),
        DataSource.tickerTypeRepoLayer,
        DataSource.historicalDataRepoLayer)
  }

  def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = appLogic

  val appLogic: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = for {
    config <- ZIO.config(HttpServerConfig.config)
    _ <- ZIO.logInfo("Starting server on http://localhost:8081")
    _ <- server(config)
  } yield ExitCode.success
}

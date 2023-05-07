package serv1.backend.db

import io.getquill.jdbczio.Quill
import io.getquill.{CompositeNamingStrategy3, Escape, SnakeCase, UpperCase}
import zio.ZLayer

object DataSource {
  type NamingStrategyType = CompositeNamingStrategy3[SnakeCase, UpperCase, Escape]

  def historicalDataRepoLayer: ZLayer[Any with Quill.Postgres[NamingStrategyType], Throwable, HistoricalDataRepo] =
    ZLayer.fromFunction(new HistoricalDataRepo(_))

  def tickerTypeRepoLayer: ZLayer[Any with Quill.Postgres[NamingStrategyType], Throwable, TickerTypeRepo] =
    ZLayer.fromFunction(new TickerTypeRepo(_))

}

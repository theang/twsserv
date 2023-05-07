package serv1.backend.db

import io.getquill._
import io.getquill.jdbczio.Quill
import serv1.backend.dto.TickerTypeRequest
import serv1.db.schema.TickerTypeDB
import serv1.model.ticker.BarSizes
import serv1.model.ticker.BarSizes.BarSize
import zio.ZIO

import java.sql.SQLException

class TickerTypeRepo(quill: Quill.Postgres[CompositeNamingStrategy3[SnakeCase, UpperCase, Escape]]) {
  final val TABLE: String = "TICKER"

  import quill._

  private implicit val barSizeDecoder: MappedEncoding[String, BarSize] = MappedEncoding[String, BarSize](BarSizes.withName)

  private def tickerTypeById(id: Int): ZIO[Any, SQLException, List[TickerTypeDB]] =
    run(quote {
      querySchema[TickerTypeDB]("\"TICKER\"").filter(_.id == lift(id))
    })

  private def tickerTypeByRequest(tickerTypeRequest: TickerTypeRequest): ZIO[Any, SQLException, List[TickerTypeDB]] =
    run(quote {
      (tickerTypeRequest: TickerTypeRequest) =>
        querySchema[TickerTypeDB]("\"TICKER\"").filter {
          tickerType => tickerTypeRequest.id.forall(tickerType.id == _) && tickerTypeRequest.name.forall(tickerType.name == _) && tickerTypeRequest.exchange.forall(tickerType.exchange == _)
        }
    }(lift(tickerTypeRequest)))

  private def tickerTypes: ZIO[Any, SQLException, List[TickerTypeDB]] = run(quote {
    querySchema[TickerTypeDB]("\"TICKER\"")
  })
}

object TickerTypeRepo {
  def tickerTypeById(id: Integer): ZIO[TickerTypeRepo, SQLException, List[TickerTypeDB]] =
    ZIO.serviceWithZIO[TickerTypeRepo](_.tickerTypeById(id))

  def tickerTypeByRequest(tickerTypeRequest: TickerTypeRequest): ZIO[TickerTypeRepo, SQLException, List[TickerTypeDB]] =
    ZIO.serviceWithZIO[TickerTypeRepo](_.tickerTypeByRequest(tickerTypeRequest))

  def tickerTypes: ZIO[TickerTypeRepo, SQLException, List[TickerTypeDB]] = ZIO.serviceWithZIO[TickerTypeRepo](_.tickerTypes)
}

package serv1.db.types

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

object HistoricalDataType extends Enumeration {
  protected case class HistoricalDataTypeVal(intVal: Int) extends super.Val

  type HistoricalDataType = Value
  val TRADES: HistoricalDataType = HistoricalDataTypeVal(0)
  val HISTORICAL_VOLATILITY: HistoricalDataType = HistoricalDataTypeVal(6)
  val OPTION_IMPLIED_VOLATILITY: HistoricalDataType = HistoricalDataTypeVal(7)
  val AGGTRADES: HistoricalDataType = HistoricalDataTypeVal(14)

  val intToHistoricalDataTypeMap: Map[Int, HistoricalDataType] = HistoricalDataType.values.map {
    v => (v.asInstanceOf[HistoricalDataTypeVal].intVal, v)
  }.toMap

  implicit val historicalDataTypeValMapper: JdbcType[Value] with BaseTypedType[Value] = MappedColumnType.base[Value, Int](_.asInstanceOf[HistoricalDataTypeVal].intVal, intToHistoricalDataTypeMap(_))
}

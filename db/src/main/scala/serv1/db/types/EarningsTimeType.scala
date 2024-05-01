package serv1.db.types

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

object EarningsTimeType extends Enumeration {
  protected case class EarningsTimeTypeVal(intVal: Int) extends super.Val

  type EarningsTimeType = Value
  val TIME_NOT_SUPPLIED: EarningsTimeType = EarningsTimeTypeVal(0)
  val TIME_PRE_MARKET: EarningsTimeType = EarningsTimeTypeVal(1)
  val TIME_AFTER_HOURS: EarningsTimeType = EarningsTimeTypeVal(2)

  val intToEarningsTimeType: Map[Int, EarningsTimeType] = EarningsTimeType.values.map {
    v => (v.asInstanceOf[EarningsTimeTypeVal].intVal, v)
  }.toMap

  implicit val earningsTimeTypeValMapper: JdbcType[Value] with BaseTypedType[Value] = MappedColumnType.base[Value, Int](_.asInstanceOf[EarningsTimeTypeVal].intVal, intToEarningsTimeType(_))
}

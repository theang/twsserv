package serv1.model.ticker

import serv1.db.types.DatabaseEnumeration

object BarSizes extends DatabaseEnumeration {
  type BarSize = Value

  val MIN1, MIN5, MIN15, HOUR, DAY, TICK = Value
}

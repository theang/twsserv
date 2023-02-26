package serv1.db.types

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

abstract class DatabaseEnumeration extends Enumeration {
  implicit val enumerationMapper: JdbcType[Value] with BaseTypedType[Value] = MappedColumnType.base[Value, String](_.toString, this.withName)
}

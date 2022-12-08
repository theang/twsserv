package serv1.db.types

import slick.jdbc.PostgresProfile.api._

abstract class DatabaseEnumeration extends Enumeration {
    implicit val enumerationMapper = MappedColumnType.base[Value, String](_.toString, this.withName)
}

package serv1.db

import org.postgresql.util.PSQLException
import serv1.db.DBSelectRawForUpdate._
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{SQLActionBuilder, SetParameter}
import slick.sql.SqlStreamingAction

import scala.concurrent.{ExecutionContext, Future}

object DBSelectRawForUpdate {
  sealed trait FailureReason

  case object AlreadyLocked extends FailureReason

  case object NothingToLock extends FailureReason

  sealed trait Result[T]

  case class Failure[T](reason: FailureReason) extends Result[T]

  case class Success[T](result: T) extends Result[T]
}

/**
 * Use to prepend SELECT 1 FROM (...) base FOR UPDATE
 * before running 'action'
 *
 * Two variants:
 * 1. if 'wait' is 0 then SELECT 1 FROM (...) base FOR UPDATE NO WAIT
 * called
 * 2. if 'wait' > 0  then
 * SET LOCAL lock_timeout = '${wait}s';
 * SELECT 1 FROM (...) base FOR UPDATE
 * is called
 *
 * @param query - should be a query for single row, as 'action' will be called on that row
 * @param db    - Database
 * @param ec    - ExecutionContext
 * @tparam E - query parameter
 * @tparam U - query result type
 */
class DBSelectRawForUpdate[E, U](query: Query[E, U, Seq])(implicit db: Database, ec: ExecutionContext) {

  def apply[R](action: U => DBIO[R], wait: Int = 0): Future[Result[R]] = {
    recoverLockNotAvailable(lock(action, wait))
  }

  def recoverLockNotAvailable[R](result: Future[Result[R]]): Future[Result[R]] = {
    result.recover {
      case e: PSQLException =>
        e.getServerErrorMessage.getSQLState match {
          case DBErrors.LOCK_NOT_AVAILABLE => Failure(AlreadyLocked)
        }
    }
  }

  def executeEffect[R](effect: U => DBIO[R]): DBIO[Result[R]] = {
    query.result.headOption.flatMap {
      case Some(row) =>
        effect(row).map(Success[R])
      case _ => DBIO.successful(Failure[R](NothingToLock))
    }
  }

  def lock[R](effect: U => DBIO[R], wait: Int = 0): Future[Result[R]] = {
    db.run(
      (if (wait > 0) {
        lockAction(wait).flatMap { _ =>
          executeEffect(effect)
        }
      } else {
        lockActionNoWait.flatMap { _ =>
          executeEffect(effect)
        }
      }).transactionally
    )
  }

  def lockActionNoWait: DBIO[Option[Int]] = {
    val baseSql = query.result.statements.head
    sql"SELECT 1 FROM (#$baseSql) base FOR UPDATE NOWAIT".as[Int].headOption
  }

  def setWaitAction(wait: Int): SqlStreamingAction[Vector[Unit], Unit, Effect] = {
    val setLocalTimeout: String = s"SET LOCAL lock_timeout = '${wait}s';"
    SQLActionBuilder(Seq(setLocalTimeout), SetParameter.SetUnit).as[Unit]
  }

  def lockAction(wait: Int): DBIO[Option[Int]] = {
    val rowSelectionSql = query.result.statements.head
    setWaitAction(wait).flatMap { _ =>
      sql"SELECT 1 FROM (#$rowSelectionSql) base FOR UPDATE".as[Int].headOption
    }
  }
}

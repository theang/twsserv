package serv1.db.repo.impl

import serv1.db.DB
import serv1.db.exception.DatabaseException
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.util.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

trait BaseRepo extends Logging {
  type ActionType = PostgresProfile.ProfileAction[PostgresProfile.InsertActionExtensionMethods[_]#MultiInsertResult, NoStream, Effect.Write]

  def writeWithCheck(expectedRows: Int, action: ActionType, timeout: Duration): Unit = {
    Await.result(DB.db.run(action.asTry), timeout) match {
      case Success(Some(insertedRows)) =>
        if (expectedRows != insertedRows) {
          val msg = s"Insert failed: (toInsert) $expectedRows != (inserted) $insertedRows"
          logger.error(msg)
          throw new DatabaseException(message = msg)
        }
      case Success(None) =>
        val msg = s"Insert returned None"
        logger.error(msg)
        throw new DatabaseException(message = msg)
      case Failure(ex) =>
        logger.error(s"Insert has exception", ex)
        throw new DatabaseException(cause = ex)
    }
  }
}

package serv1.app

import akka.actor.typed.{ActorSystem, Props}
import akka.http.scaladsl.server.RouteConcatenation
import serv1.db.DB
import serv1.db.repo.TickerDataRepo
import serv1.rest.RestServer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object App extends RouteConcatenation {
  def main(args: Array[String]): Unit = {
    DB.createTables()
    TickerDataRepo.init()
    val system = ActorSystem[RestServer.Message](RestServer("localhost", 8080), "rest-server")
    StdIn.readLine() // let it run until user presses return
    system.tell(RestServer.Stop)
    Await.ready(system.whenTerminated, 1.minute)
  }
}

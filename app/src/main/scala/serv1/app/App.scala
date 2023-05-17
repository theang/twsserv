package serv1.app

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.RouteConcatenation
import com.typesafe.config.Config
import serv1.config.ServConfig
import serv1.db.DB
import serv1.db.repo.impl.TickerDataRepo
import serv1.rest.RestServer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object App extends RouteConcatenation {
  var config: Config = ServConfig.config.getConfig("server")
  var port: Int = if (config.getIsNull("port")) 8080 else config.getInt("port")

  def main(args: Array[String]): Unit = {
    DB.createTables()
    TickerDataRepo.init()
    val system = ActorSystem[RestServer.Message](RestServer("localhost", port), "rest-server")
    StdIn.readLine() // let it run until user presses return
    system.tell(RestServer.Stop)
    Await.ready(system.whenTerminated, 1.minute)
  }
}

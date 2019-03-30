package users

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import cats.data._
import cats.implicits._
import users.api.UserApiRoutes
import users.config._
import users.main._
import scala.concurrent.Future
import scala.io.StdIn

object Main extends App with UserApiRoutes {

  val config = ApplicationConfig(
    executors = ExecutorsConfig(
      services = ExecutorsConfig.ServicesConfig(
        parallellism = 4
      )
    ),
    services = ServicesConfig(
      users = ServicesConfig.UsersConfig(
        failureProbability = 0.0,
        timeoutProbability = 0.0
      )
    )
  )

  val application = Application.fromApplicationConfig.run(config)

  override val userManagement = application.services.userManagement
  override val apiVersion = "v1"

  implicit val system = ActorSystem("UsersApi")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val serverBindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => ex.printStackTrace() }
      system.terminate()
    }

}

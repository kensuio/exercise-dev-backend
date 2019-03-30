package users.api

import akka.http.scaladsl.server.Route
import users.services.UserManagement
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete

object UserApiRoutes {

  final case class UserData(
      id: String,
      name: String,
      email: String,
      status:  String,
      password: Option[String],
      version: Int
  )

  final case class SignUpData(
      name: String,
      email: String,
      password: Option[String],
  )

  final case class PasswordData(password: Option[String])
  final case class StatusData(status: String)
}

trait UserApiRoutes extends JsonSupport {

  val UserVersionHeader = "User-Version"

  val apiVersion: String

  val userManagement: UserManagement[Future[?]]

  lazy val routes: Route = path("api" / apiVersion / "users"/ Segment) { id =>
    get {
      complete("Hello")
    }
  }
}

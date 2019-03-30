package users.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import users.domain.User
import users.services.UserManagement
import users.services.usermanagement.Error
import scala.concurrent.Future
import scala.util.{Failure, Success}

object UserApiRoutes {

  final case class UserData(
      id: String,
      name: String,
      email: String,
      status:  String,
      password: Option[String],
      version: Int
  )

  object UserData {
    def apply(u: User): UserData = UserData(
      id = u.id.value,
      name = u.userName.value,
      email = u.emailAddress.value,
      status = u.status.toString,
      password = u.password.map(_.toString),
      version = u.metadata.version
    )
  }

  final case class SignUpData(
      name: String,
      email: String,
      password: Option[String],
  )

  final case class PasswordData(password: Option[String])
  final case class StatusData(status: String)

  case class Fail(err: String)
}

trait UserApiRoutes extends JsonSupport {

  import UserApiRoutes._

  val UserVersionHeader = "User-Version"

  val apiVersion: String

  val userManagement: UserManagement[Future[?]]

  lazy val routes: Route = path("api" / apiVersion / "users"/ Segment) { id =>
    get {
      onComplete(userManagement.get(User.Id(id))) {
        case Success(Right(u)) => complete((StatusCodes.OK, UserData(u)))
        case Success(Left(err)) => err match {
          case Error.NotFound => complete((
            StatusCodes.NotFound,
            Fail(s"No user found for id: $id")
          ))
          case _ => complete((
            StatusCodes.InternalServerError, Fail("Unknown Server error")))
        }
        case Failure(ex) => complete((
          StatusCodes.InternalServerError,
          Fail(ex.getMessage)
        ))
      }
    }
  }
}

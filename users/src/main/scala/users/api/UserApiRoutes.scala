package users.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import users.api.RouteHandling.Fail
import users.domain._
import users.services.UserManagement
import users.services.usermanagement.Error
import scala.concurrent.Future

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
}

trait UserApiRoutes extends JsonSupport with RouteHandling {

  import UserApiRoutes._

  val UserVersionHeader = "User-Version"

  val apiVersion: String

  val userManagement: UserManagement[Future[?]]

  lazy val routes: Route = path("api" / apiVersion / "users"/ Segment) { id =>
    get {
      handle(userManagement.get(User.Id(id))) {
        case Right(u) => complete((StatusCodes.OK, UserData(u)))
        case Left(err) => handleLeft(err) {
          case Error.NotFound => complete((
            StatusCodes.NotFound,
            Fail(s"No user found for id: $id")
          ))
        }
      }
    }
  } ~ path("api" / apiVersion / "users") {
    post {
      entity(as[SignUpData]) { su =>
        handle(userManagement.signUp(
          UserName(su.name),
          EmailAddress(su.email),
          su.password.map(Password)
        )) {
          case Right(u) => complete((StatusCodes.OK, UserData(u)))
          case Left(err) => handleLeft(err) {
            case Error.Exists => complete((
              StatusCodes.Conflict,
              Fail(s"User with such name already exists: ${su.name}")
            ))
          }
        }
      }
    }
  }
}

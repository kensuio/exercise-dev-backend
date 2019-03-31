package users.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.{Route, StandardRoute}
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

  final case class UserDataList(list: List[UserData])

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

  final case class EmailData(email: String)
  final case class PasswordData(password: Option[String])
  final case class StatusData(status: String)

  val StatusActive = "active"
  val StatusBlocked = "blocked"
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
          case Error.NotFound => notFound(id)
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
  } ~ path("api" / apiVersion / "users" / Segment / "email") { id =>
    put {
      entity(as[EmailData]) { ed =>
        handle(userManagement.updateEmail(
          User.Id(id),
          EmailAddress(ed.email))
        ) {
          case Right(u) => respondWithHeader(
            RawHeader(UserVersionHeader, u.metadata.version.toString)) {
              complete(StatusCodes.NoContent)
            }
          case Left(err) => handleLeft(err) {
            case Error.NotFound => notFound(id)
            case Error.Deleted => gone(id)
          }
        }
      }
    }
  } ~ path("api" / apiVersion / "users" / Segment / "password") { id =>
    put {
      entity(as[PasswordData]) { pd =>
        pd.password match {
          case Some(pass) if pass.nonEmpty =>
            handle(
              userManagement.updatePassword(User.Id(id), Password(pass))
            ) {
            case Right(u) => respondWithHeader(
              RawHeader(UserVersionHeader, u.metadata.version.toString)) {
              complete(StatusCodes.NoContent)
            }
            case Left(err) =>  handleLeft(err) {
              case Error.NotFound => notFound(id)
              case Error.Deleted => gone(id)
            }
          }
          case _ => handle(userManagement.resetPassword( User.Id(id))) {
            case Right(u) => respondWithHeader(
              RawHeader(UserVersionHeader, u.metadata.version.toString)) {
              complete(StatusCodes.NoContent)
            }
            case Left(err) =>  handleLeft(err) {
              case Error.NotFound => notFound(id)
              case Error.Deleted => gone(id)
            }
          }
        }
      }
    }
  } ~ path("api"/ "admin" / apiVersion / "users" / Segment / "status") { id =>
    put {
      entity(as[StatusData]) { sd =>
        val status = sd.status.toLowerCase
        status match {
          case StatusActive  => handle(userManagement.unblock(User.Id(id))) {
            case Right(u) => respondWithHeader(
              RawHeader(UserVersionHeader, u.metadata.version.toString)) {
              complete(StatusCodes.NoContent)
            }
            case Left(err) =>  handleLeft(err) {
              case Error.NotFound => notFound(id)
              case Error.Deleted => gone(id)
              case Error.Active => complete((
                StatusCodes.Conflict,
                Fail("Cannot unblock active user")))
            }
          }
          case StatusBlocked => handle(userManagement.block(User.Id(id))) {
            case Right(u) => respondWithHeader(
              RawHeader(UserVersionHeader, u.metadata.version.toString)) {
              complete(StatusCodes.NoContent)
            }
            case Left(err) =>  handleLeft(err) {
              case Error.NotFound => notFound(id)
              case Error.Deleted => gone(id)
              case Error.Blocked => complete((
                StatusCodes.Conflict,
                Fail("Cannot block blocked user")))
            }
          }
          case _ => complete((
            StatusCodes.BadRequest,
            Fail("`Active` or `Blocked` values allowed here")))
        }
      }
    }
  } ~ path("api" / "admin" / apiVersion / "users"/ Segment) { id =>
    delete {
      handle(userManagement.delete(User.Id(id))) {
        case Right(u) => complete(StatusCodes.NoContent)
        case Left(err) => handleLeft(err) {
          case Error.NotFound => notFound(id)
          case Error.Deleted => gone(id)
          case Error.Active => complete((
            StatusCodes.Conflict,
            Fail("Cannot block active user")))
        }
      }
    }
  } ~ path("api" / "admin" / apiVersion / "users") {
    get {
      handle(userManagement.all()) {
        case Right(us) =>
          complete((StatusCodes.OK, UserDataList(us.map(UserData(_)))))
        case Left(_) => complete((
            StatusCodes.InternalServerError, Fail("Unknown Server error")))
      }
    }
  }


  private def notFound(id: String): StandardRoute = complete((
      StatusCodes.NotFound, Fail(s"No user found for id: $id")))

  private def gone(id: String): StandardRoute = complete((
      StatusCodes.Gone, Fail(s"User deleted for id: $id")))
}

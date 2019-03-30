package users.api
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{onComplete, withRequestTimeout}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import spray.json.DefaultJsonProtocol
import users.services.usermanagement.Error
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RouteHandling {
  case class Fail(err: String)
}

trait RouteHandling extends SprayJsonSupport {

  import DefaultJsonProtocol._
  import RouteHandling._

  implicit val failFormat = jsonFormat1(Fail)

  def handle[T](
      future: ⇒ Future[Error Either T])(
      f: Error Either T => Route
  ): Route =  {
    withRequestTimeout(1.seconds) {
      onComplete(future) {
        case Success(r) => f(r)
        case Failure(ex) =>
          val msg = Option(ex.getMessage).getOrElse("Unknown Server error")
          complete((
            StatusCodes.InternalServerError, Fail(msg)))
      }
    }
  }

  def handleLeft[T](
      left: Error)(
      pf: PartialFunction[Error, Route]
  ): Route = {
    val err = (e: Error) => complete((
      StatusCodes.InternalServerError, Fail("Unknown Server error")))
    pf.applyOrElse(left, err)
  }
}

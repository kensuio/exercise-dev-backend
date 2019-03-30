package users.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {

  import DefaultJsonProtocol._
  import UserApiRoutes._

  implicit val userDataFormat = jsonFormat6(UserData.apply)
  implicit val signUpDataFormat = jsonFormat3(SignUpData)
  implicit val passwordDataFormat = jsonFormat1(PasswordData)
  implicit val statusDataFormat = jsonFormat1(StatusData)
  implicit val failFormat = jsonFormat1(Fail)
}

package users.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {

  import DefaultJsonProtocol._
  import UserApiRoutes._

  implicit val userDataFormat = jsonFormat6(UserData.apply)
  implicit val userDataLIst = jsonFormat1(UserDataList)
  implicit val signUpDataFormat = jsonFormat3(SignUpData)
  implicit val passwordDataFormat = jsonFormat1(PasswordData)
  implicit val emailFormay = jsonFormat1(EmailData)
  implicit val statusDataFormat = jsonFormat1(StatusData)
}

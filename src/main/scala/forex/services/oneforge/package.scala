package forex.services

import scala.util.control.NoStackTrace
import izumi.reflect.Tag

package object oneforge {
  /* Custom errors for the client calls seen/handled by the upstream service caller */

  sealed trait OneForgeError extends Throwable with NoStackTrace


  object OneForgeError {
    implicit val errorTag: Tag[OneForgeError] = Tag.tagFromTagMacro
    final case class Generic(message: String = "") extends OneForgeError {
      override def toString(): String = s"OneForgeError: ${message}."
    }
    final case object GeneratedURLWasMalformed extends OneForgeError {
      override def toString(): String = s"Could not generate URL for 1forge API because the result was malformed. Check configuration and url-building."
    }
    final case class CommunicationError(inner: Throwable) extends OneForgeError {
      override def toString(): String = s"Error in communication with 1forge API: ${inner.getMessage}."
    }
    final case class InvalidRequestError(url: String, body: String) extends OneForgeError {
      override def toString(): String = s"Invalid request to 1forge API with URL $url. Response was: $body."
    }
    final case class ServerError(body: String) extends OneForgeError {
      override def toString(): String = s"Server error from 1forge API. Response was: $body."
    }
    final case class ResponseParsingError(responseBody: String) extends OneForgeError {
      override def toString(): String = s"Error parsing response from 1forge API. Response was: $responseBody. Check model and decoder against documentation and response."
    }
    final case class System(underlying: Throwable) extends OneForgeError
  }
}

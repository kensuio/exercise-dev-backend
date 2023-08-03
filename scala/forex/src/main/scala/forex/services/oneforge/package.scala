package forex.services

import scala.util.control.NoStackTrace
import izumi.reflect.Tag

package object oneforge {
  /* Custom errors for the client calls seen/handled by the upstream service caller */

  sealed trait OneForgeError extends Throwable with NoStackTrace


  object OneForgeError {
    implicit val errorTag: Tag[OneForgeError] = Tag.tagFromTagMacro
    final case class Generic(message: String = "") extends OneForgeError {
      override def toString(): String = s"OneForgeError: ${message}"
    }
    final case class System(underlying: Throwable) extends OneForgeError
  }
}

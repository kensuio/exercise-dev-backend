package forex.services

import scala.util.control.NoStackTrace

package object oneforge {
  /* Custom errors for the client calls seen/handled by the upstream service caller */

  sealed trait OneForgeError extends Throwable with NoStackTrace

  object OneForgeError {
    final case object Generic extends OneForgeError
    final case class System(underlying: Throwable) extends OneForgeError
  }
}

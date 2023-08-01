package forex.services

import forex.domain.Rate.Pair

import scala.util.control.NoStackTrace

package object oneforge {
  /* Custom errors for the client calls seen/handled by the upstream service caller */

  sealed trait OneForgeError extends Throwable with NoStackTrace

  object OneForgeError {
    final case object Generic extends OneForgeError

    final case class System(underlying: Throwable) extends OneForgeError

    final case class MissingPair(pair: Pair) extends OneForgeError

    final case class Http(message: String, statusCode: Int) extends OneForgeError

    final case class Deserialization(body: String, message: String) extends OneForgeError
  }
}

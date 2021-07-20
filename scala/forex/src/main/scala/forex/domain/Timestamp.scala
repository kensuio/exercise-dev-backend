package forex.domain

import java.time.OffsetDateTime

import io.circe._
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder

final case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]
}

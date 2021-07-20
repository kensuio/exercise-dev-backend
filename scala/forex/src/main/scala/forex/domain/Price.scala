package forex.domain

import io.circe._
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder

final case class Price(value: BigDecimal) extends AnyVal

object Price {

  def apply(value: Int): Price =
    Price(BigDecimal(value))

  implicit val encoder: Encoder[Price] = deriveUnwrappedEncoder[Price]
}

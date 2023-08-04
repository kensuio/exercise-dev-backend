package forex.domain

import zio.json.JsonEncoder
import java.math.MathContext

final case class Price(value: BigDecimal) extends AnyVal

object Price {

  /** Custom constructor based on primitives */
  def apply(value: Int): Price =
    Price(BigDecimal(value, new MathContext(6))) 

  implicit val encoder: JsonEncoder[Price] = JsonEncoder[BigDecimal].contramap(_.value)
}

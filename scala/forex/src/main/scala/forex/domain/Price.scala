package forex.domain

import zio.json.JsonEncoder

final case class Price(value: BigDecimal) extends AnyVal

object Price {

  def apply(value: Int): Price =
    Price(BigDecimal(value))

  implicit val encoder: JsonEncoder[Price] = JsonEncoder[BigDecimal].contramap(_.value)
}

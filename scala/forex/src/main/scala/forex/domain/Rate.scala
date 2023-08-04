package forex.domain

import java.time.OffsetDateTime

import zio.json.{DeriveJsonEncoder, JsonEncoder}
import java.math.MathContext

/**
 * The quotation for a given exchange rate
 *
 * @param pair the exchanged currencies
 * @param price the conversion price
 * @param timestamp the quotation time
 */
final case class Rate(
  pair: Rate.Pair,
  price: Price,
  timestamp: OffsetDateTime
) {
  /** For optimization purposes. We can return an inverted cached rate for an inverted order of queried currencies. */
  def invert: Rate = Rate(
    Rate.Pair(pair.to, pair.from), 
    Price((BigDecimal("1.0", new MathContext(6)) / price.value)), 
    timestamp
  )
}

object Rate {

  /** Let's define a useful wrapper with domain meaning */
  final case class Pair(
    from: Currency,
    to: Currency
  ) {
    /** The pair in canonical ordering - for caching-optimization configurable by `canonicalize` */
    def canonicalized(implicit ord: Ordering[Currency]): Pair = if (ord.lt(from, to)) this else Pair(to, from)
  }

  object Pair {
    implicit val encoder: JsonEncoder[Pair] = DeriveJsonEncoder.gen[Pair]
  }

  implicit val encoder: JsonEncoder[Rate] = DeriveJsonEncoder.gen[Rate]
}

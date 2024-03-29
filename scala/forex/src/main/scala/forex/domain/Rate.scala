package forex.domain

import java.time.OffsetDateTime

import zio.json.{DeriveJsonEncoder, JsonEncoder}

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
)

object Rate {

  /** Let's define a useful wrapper with domain meaning */
  final case class Pair(
    from: Currency,
    to: Currency
  )

  object Pair {

    implicit val encoder: JsonEncoder[Pair] = DeriveJsonEncoder.gen[Pair]
  }

  implicit val encoder: JsonEncoder[Rate] = DeriveJsonEncoder.gen[Rate]
}

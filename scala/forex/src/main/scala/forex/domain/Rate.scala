package forex.domain

import java.time.OffsetDateTime

import zio.json.{DeriveJsonEncoder, JsonEncoder}

final case class Rate(
  pair: Rate.Pair,
  price: Price,
  timestamp: OffsetDateTime
)

object Rate {

  final case class Pair(
    from: Currency,
    to: Currency
  )

  object Pair {

    implicit val encoder: JsonEncoder[Pair] = DeriveJsonEncoder.gen[Pair]
  }

  implicit val encoder: JsonEncoder[Rate] = DeriveJsonEncoder.gen[Rate]
}

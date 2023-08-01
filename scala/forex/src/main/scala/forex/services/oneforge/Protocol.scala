package forex.services.oneforge

import zio.json.{DeriveJsonDecoder, JsonDecoder}

object Protocol {

  final case class OneForgeRate(
    p: BigDecimal,
    a: BigDecimal,
    b: BigDecimal,
    s: String,
    t: Long
  )

  object OneForgeRate {
    implicit val decoder: JsonDecoder[OneForgeRate] = DeriveJsonDecoder.gen[OneForgeRate]
  }
}

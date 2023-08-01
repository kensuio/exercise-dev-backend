package forex.services.oneforge

import forex.domain.{Currency, Price, Rate}
import forex.domain.Rate.Pair
import forex.services.oneforge.Protocol.OneForgeRate
import io.scalaland.chimney.dsl._

import java.time.{Instant, OffsetDateTime, ZoneOffset}

object Converters {

  def toRate(response: OneForgeRate): Rate = response.into[Rate]
    .withFieldComputed(_.pair, r => extractPair(r.s))
    .withFieldComputed(_.price, r => Price(r.p))
    .withFieldComputed(_.timestamp, r => timestampToOffsetDateTime(r.t))
    .transform

  private def extractPair(s: String): Pair = {
    val Array(from, to) = s.split("/")
    Pair(Currency.fromString(from), Currency.fromString(to))
  }

  private def timestampToOffsetDateTime(timestampInMillis: Long): OffsetDateTime = {
    val instant = Instant.ofEpochMilli(timestampInMillis)
    instant.atOffset(ZoneOffset.UTC)
  }
}

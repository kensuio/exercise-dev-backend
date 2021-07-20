package forex.interfaces.api.rates

import forex.domain._
import forex.processes.rates.GetRequest

object Converters {
  import Protocol._

  def toGetRequest(request: GetApiRequest): GetRequest =
    GetRequest(request.from, request.to)

  def toGetApiResponse(rate: Rate): GetApiResponse =
    GetApiResponse(
      from      = rate.pair.from,
      to        = rate.pair.to,
      price     = rate.price,
      timestamp = rate.timestamp
    )
}

package forex.config

import zio.config.magnolia.DeriveConfigDescriptor
import zio.duration.Duration

final case class ApplicationConfig(
  akka: AkkaConfig,
  api: ApiConfig
)

object ApplicationConfig {
  val descriptor = DeriveConfigDescriptor.descriptor[ApplicationConfig]
}

final case class AkkaConfig(
  name: String,
  exitJvmTimeout: Duration
)

final case class ApiConfig(
  interface: String,
  port: Int
)

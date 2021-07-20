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
  exitJvmTimeout: Option[Duration]
)

final case class ApiConfig(
  interface: String,
  port: Int
)

final case class ExecutorsConfig(
  default: String
)

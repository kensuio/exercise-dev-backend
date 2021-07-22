package forex.config

import zio.config.magnolia.DeriveConfigDescriptor
import zio.duration.Duration

/* Here we collect the definitions that represent configurations needed to
 * run the application, as composite object trees
 */

/** Overall config */
final case class ApplicationConfig(
  akka: AkkaConfig,
  api: ApiConfig
)

object ApplicationConfig {

  /** Provides configuration using [[zio.config]] */
  val descriptor = DeriveConfigDescriptor.descriptor[ApplicationConfig]
}

/** Needed to run the akka system */
final case class AkkaConfig(
  name: String,
  exitJvmTimeout: Duration
)

/** Where to sun the server */
final case class ApiConfig(
  interface: String,
  port: Int
)

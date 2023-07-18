package forex.config

import zio.config.magnolia.Descriptor
import zio.Duration

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
  val descriptor = Descriptor.descriptor[ApplicationConfig]
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

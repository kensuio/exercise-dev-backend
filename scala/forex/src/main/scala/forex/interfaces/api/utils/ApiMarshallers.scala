package forex.interfaces.api.utils

import de.heikoseeberger.akkahttpziojson.ZioJsonSupport
import forex.interfaces.api.utils.marshalling._

trait ApiMarshallers extends ZioSupport with ZioJsonSupport

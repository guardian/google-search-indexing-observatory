package ophan.google.index.observatory.model

import java.time.Instant

case class AvailabilityRecord(capiId: String, missing: Option[Instant], found: Option[Instant]) {
  val contentHasBeenFound: Boolean = found.nonEmpty
}


package ophan.google.index.checker.model

import java.time.Instant
import scala.collection.immutable.SortedSet

case class AvailabilityRecord(capiId: String, missing: Set[Instant], found: Set[Instant]) {
  val contentHasBeenFound: Boolean = found.nonEmpty
}


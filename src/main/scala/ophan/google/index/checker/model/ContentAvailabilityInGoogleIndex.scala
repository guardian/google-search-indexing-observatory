package ophan.google.index.checker.model

import java.time.Clock.systemUTC
import java.time.Duration.ofMinutes
import java.time.{Clock, Duration, Instant}
import scala.collection.immutable.SortedMap
import scala.math.Ordering.Implicits._

case class ContentAvailabilityInGoogleIndex(
  contentSummary: ContentSummary,
  indexPresenceByTime: SortedMap[Instant, Boolean]
) {
  def contentIsCurrentlyWorryinglyAbsentFromGoogle(gracePeriod: Duration = ofMinutes(1))(implicit clock: Clock = systemUTC): Boolean = {
    contentWasAbsentFromGoogleAtLatestCheck && contentSummary.timeSinceUrlWentPublic() > gracePeriod
  }

  val contentWasAbsentFromGoogleAtLatestCheck = indexPresenceByTime.lastOption.exists(_._2 == false)
}

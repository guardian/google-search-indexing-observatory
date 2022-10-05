package ophan.google.index.observatory.model

import java.time.Clock.systemUTC
import java.time.Duration.ofMinutes
import java.time.{Clock, Duration}
import scala.math.Ordering.Implicits._

case class ContentAvailabilityInGoogleIndex(
  contentSummary: ContentSummary,
  availabilityRecord: AvailabilityRecord
) {
  def contentIsCurrentlyWorryinglyAbsentFromGoogle(gracePeriod: Duration = ofMinutes(1))(implicit clock: Clock = systemUTC): Boolean = {
    !availabilityRecord.contentHasBeenFound && contentSummary.timeSinceUrlWentPublic() > gracePeriod
  }

}

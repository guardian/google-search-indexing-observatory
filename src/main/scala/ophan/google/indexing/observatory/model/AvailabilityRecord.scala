package ophan.google.indexing.observatory.model

import ophan.google.indexing.observatory.model.AvailabilityRecord.{DelayForFirstCheckAfterContentIsFirstSeenInSitemap, reasonableTimeBetweenChecksForContentAged}

import java.net.{URI, URISyntaxException}
import java.time.{Clock, Duration, Instant}
import org.scanamo._
import org.scanamo.generic.semiauto._
import org.scanamo.syntax._

import java.time.Duration.{ofMinutes, ofSeconds}
import java.time.temporal.ChronoUnit.SECONDS
import java.time.format.DateTimeParseException
import scala.math.Ordering.Implicits._

case class CheckStatus(timestamp: Instant, wasFound: Boolean)

case class AvailabilityRecord(
  uri: URI,
  firstSeenInSitemap: Instant,
  missing: Option[Instant] = None,
  found: Option[Instant] = None
) {
  val latestCheck: Option[CheckStatus] = (
    missing.map(CheckStatus(_, wasFound = false)) ++ found.map(CheckStatus(_, wasFound = true))
  ).toSeq.maxByOption(_.timestamp)

  val contentHasBeenFound: Boolean = latestCheck.exists(_.wasFound)
  val currentlyRecordedMissing: Boolean = latestCheck.exists(!_.wasFound)

  def needsCheckingNow()(implicit clock: Clock = Clock.systemUTC): Boolean = {
    !contentHasBeenFound && {
      val now = clock.instant()
      val timeSinceFirstSeenInSitemap = Duration.between(firstSeenInSitemap, now)
      timeSinceFirstSeenInSitemap > DelayForFirstCheckAfterContentIsFirstSeenInSitemap && missing.forall { m =>
        Duration.between(m, now) > reasonableTimeBetweenChecksForContentAged(timeSinceFirstSeenInSitemap)
      }
    }
  }
}

object AvailabilityRecord {
  implicit val uriAsStringFormat: DynamoFormat[URI] =
    DynamoFormat.coercedXmap[URI, String, URISyntaxException](new URI(_), _.toString)

  implicit val instantAsISO8601StringFormat: DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, DateTimeParseException](Instant.parse, _.truncatedTo(SECONDS).toString)

  implicit val formatAvailabilityRecord: DynamoFormat[AvailabilityRecord] = deriveDynamoFormat

  /** API cost-saving: I think any content that arrives in Google Search in less than 2 minutes is pretty prompt - we're
   * not  really interested in establishing delay lower than 2 minutes at this point, so it's not worth scanning content
   * less than 2 minutes old - the check is likely to come back 'missing', but it cost us an API call and do we care
   * that it's missing yet?
   *
   * Remember that the delay here will be added to the delay caused by our periodic scanning of the sitemap (currently
   * 1 minute at most).
   */
  val DelayForFirstCheckAfterContentIsFirstSeenInSitemap: Duration = ofMinutes(1)

  def reasonableTimeBetweenChecksForContentAged(age: Duration): Duration =
    ofMinutes(2).plus(age.dividedBy(5))

  object Field {
    val Uri = "uri"
    val FirstSeenInSitemap = "firstSeenInSitemap"
    def timestampFor(found: Boolean): String = if (found) "found" else "missing"
  }
}
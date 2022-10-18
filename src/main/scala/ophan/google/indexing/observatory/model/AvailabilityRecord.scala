package ophan.google.indexing.observatory.model

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

  def needsCheckingNow()(implicit clock: Clock = Clock.systemUTC): Boolean =
    !contentHasBeenFound && missing.forall(m => Duration.between(m, clock.instant()) > ofMinutes(3))
}

object AvailabilityRecord {
  implicit val uriAsStringFormat: DynamoFormat[URI] =
    DynamoFormat.coercedXmap[URI, String, URISyntaxException](new URI(_), _.toString)

  implicit val instantAsISO8601StringFormat: DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, DateTimeParseException](Instant.parse, _.truncatedTo(SECONDS).toString)

  implicit val formatAvailabilityRecord: DynamoFormat[AvailabilityRecord] = deriveDynamoFormat

  object Field {
    val Uri = "uri"
    val FirstSeenInSitemap = "firstSeenInSitemap"
    def timestampFor(found: Boolean): String = if (found) "found" else "missing"
  }
}
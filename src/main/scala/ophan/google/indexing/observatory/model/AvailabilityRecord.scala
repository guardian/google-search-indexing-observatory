package ophan.google.indexing.observatory.model

import ophan.google.indexing.observatory.Resolution
import ophan.google.indexing.observatory.model.AvailabilityRecord.Field.FirstSeenInSitemapDateIndexKey
import ophan.google.indexing.observatory.model.AvailabilityRecord.{DelayForFirstCheckAfterContentIsFirstSeenInSitemap, reasonableTimeBetweenChecksForContentAged}

import java.net.{URI, URISyntaxException}
import java.time.{Clock, Duration, Instant, LocalDate, ZoneOffset}
import org.scanamo.*
import org.scanamo.generic.semiauto.*
import org.scanamo.syntax.*

import java.time.Duration.{ofMinutes, ofSeconds}
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.SECONDS
import java.time.format.DateTimeParseException
import scala.math.Ordering.Implicits.*

case class CheckStatus(timestamp: Instant, wasFound: Boolean)

// Resolved URI? Was page HTTP 200 OK?
// Want to know if we should:
// a) ignore this page, as it was not OK - 404 or redirected too much
// b) scan an alternate url - a page which we were redirected to, which was ok
// c) scan darn url.

// pageResolvedOk
// uriAfterRedirects
case class AvailabilityRecord(
  uri: URI,
  finalUriAfterRedirects: Option[URI],
  uriResolvedOk: Boolean,
  firstSeenInSitemap: Instant,
  missing: Option[Instant] = None,
  found: Option[Instant] = None
) {
  val ultimateUri: URI = finalUriAfterRedirects.getOrElse(uri)
  
  val latestCheck: Option[CheckStatus] = (
    missing.map(CheckStatus(_, wasFound = false)) ++ found.map(CheckStatus(_, wasFound = true))
  ).toSeq.maxByOption(_.timestamp)

  val contentHasBeenFound: Boolean = latestCheck.exists(_.wasFound)
  val currentlyRecordedMissing: Boolean = latestCheck.exists(!_.wasFound)

  def needsCheckingNow()(implicit clock: Clock = Clock.systemUTC): Boolean = !contentHasBeenFound && {
    val now = clock.instant()
    val timeSinceFirstSeenInSitemap = Duration.between(firstSeenInSitemap, now)
    timeSinceFirstSeenInSitemap > DelayForFirstCheckAfterContentIsFirstSeenInSitemap && missing.forall { m =>
      Duration.between(m, now) > reasonableTimeBetweenChecksForContentAged(timeSinceFirstSeenInSitemap)
    }
  }
}

object AvailabilityRecord {

  def apply(resolved: Resolution.Resolved, firstSeenInSitemap: Instant): AvailabilityRecord = AvailabilityRecord(
    uri = resolved.redirectPath.locations.head,
    uriResolvedOk = resolved.ok,
    finalUriAfterRedirects = resolved.redirectPath.locations.tail.lastOption,
    firstSeenInSitemap = firstSeenInSitemap
  )
  
  implicit val uriAsStringFormat: DynamoFormat[URI] =
    DynamoFormat.coercedXmap[URI, String, URISyntaxException](new URI(_), _.toString)

  implicit val instantAsISO8601StringFormat: DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, DateTimeParseException](Instant.parse, _.truncatedTo(SECONDS).toString)

  implicit class RichDynamoValue(dv: DynamoValue) {
    def plusObjectEntry(keyValue: (String, String)): DynamoValue = {
      val (key, value) = keyValue
      dv.withObject(_.add(key, DynamoValue.fromString(value)).toDynamoValue)
    }
  }

  implicit class RichInstant(instant: Instant) {
    lazy val utcLocalDate: LocalDate = instant.atZone(UTC).toLocalDate
  }
  
  def firstSeenDateKeyFor(firstSeen: Instant): String = s"${firstSeen.utcLocalDate}Z"
  
  implicit val formatAvailabilityRecord: DynamoFormat[AvailabilityRecord] = new DynamoFormat[AvailabilityRecord] {
    private val standard = deriveDynamoFormat[AvailabilityRecord]

    def read(dv: DynamoValue): Either[DynamoReadError, AvailabilityRecord] = standard.read(dv)

    def write(p: AvailabilityRecord): DynamoValue =
      standard.write(p).plusObjectEntry(FirstSeenInSitemapDateIndexKey -> firstSeenDateKeyFor(p.firstSeenInSitemap))
  }

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
    val FirstSeenInSitemapDateIndexKey = s"${FirstSeenInSitemap}Date"

    def timestampFor(found: Boolean): String = if (found) "found" else "missing"
  }
}
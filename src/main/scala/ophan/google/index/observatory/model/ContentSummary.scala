package ophan.google.index.observatory.model

import com.gu.contentapi.client.model.v1.Content

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Clock.systemUTC
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MINUTES
import java.time.{Clock, Duration, Instant}
import scala.math.Ordering.Implicits._

case class ContentSummary(
  id: String,
  firstPublished: Instant,
  webUrl: URI
) {
  /**
   * This string should be something that, when you type it into Google, you
   * reliably should get this content as one of the top hits. The headline of
   * the article would be one candidate for the value, but the headlines can
   * contain characters that are difficult to escape, eg quotes & double-quotes.
   * The path of the webUrl is fairly reliable, so far as I can see.
   */
  val reliableSearchTerm: String = s""""${webUrl.getPath}""""

  val googleSearchUiUrl: URI = URI.create(s"https://www.google.com/search?q=${URLEncoder.encode(reliableSearchTerm, UTF_8)}")

  val ophanUrl: URI = URI.create(s"https://dashboard.ophan.co.uk/info?capi-id=$id")

  def timeSinceUrlWentPublic()(implicit clock: Clock = systemUTC): Duration =
    Duration.between(firstPublished, clock.instant())

  def shouldBeCheckedNowGivenExisting(availabilityRecord: AvailabilityRecord)(implicit clock: Clock): Boolean = {
    !availabilityRecord.contentHasBeenFound &&
      !availabilityRecord.missing.maxOption.exists(_ > clock.instant().minus(3, MINUTES))
  }
}

object ContentSummary {
  def from(content: Content): Option[ContentSummary] = for {
    fields <- content.fields
    firstPublished <- fields.firstPublicationDate
  } yield ContentSummary(content.id, Instant.ofEpochMilli(firstPublished.dateTime), URI.create(content.webUrl))

}
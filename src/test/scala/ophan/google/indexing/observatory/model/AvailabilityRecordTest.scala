package ophan.google.indexing.observatory.model

import com.gu.http.redirect.resolver.{Conclusion, RedirectPath, Resolution}
import ophan.google.indexing.observatory.literals.uri
import ophan.google.indexing.observatory.model.AvailabilityRecord.Field.FirstSeenInSitemapDateIndexKey
import ophan.google.indexing.observatory.model.AvailabilityRecord.{DelayForFirstCheckAfterContentIsFirstSeenInSitemap, reasonableTimeBetweenChecksForContentAged}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scanamo.{DynamoObject, DynamoValue}

import java.net.URI
import java.time.{Duration, Instant}
import java.time.Duration.{ofHours, ofMinutes, ofSeconds}
import java.time.temporal.ChronoUnit.SECONDS
import scala.math.Ordering.Implicits.*

class AvailabilityRecordTest extends AnyFlatSpec with Matchers with OptionValues  with ScalaFutures with IntegrationPatience {

  val elapsedTimeAtEachCheck: LazyList[Duration] = LazyList.iterate(DelayForFirstCheckAfterContentIsFirstSeenInSitemap) { accumulatedTime =>
    accumulatedTime.plus(reasonableTimeBetweenChecksForContentAged(accumulatedTime))
  }

  def maxNumChecksForContentMissing(duration: Duration): Int = elapsedTimeAtEachCheck.indexWhere(_ > duration)

  it should "not cost too much in API calls, but also check often enough for reasonable detail" in {
    reasonableTimeBetweenChecksForContentAged(ofSeconds(1)) should
      (be >= ofMinutes(1) and be <= ofMinutes(3))

    reasonableTimeBetweenChecksForContentAged(ofMinutes(2)) should
      (be >= ofMinutes(2) and be <= ofMinutes(3))

    reasonableTimeBetweenChecksForContentAged(ofHours(1)) should
      (be >= ofMinutes(10) and be <= ofMinutes(30))

    maxNumChecksForContentMissing(ofMinutes(1)) shouldBe 1

    maxNumChecksForContentMissing(ofHours(1)) should be <= 12
    maxNumChecksForContentMissing(ofHours(2)) should be <= 15

    val elapsedTimesAtEachCheck =
      elapsedTimeAtEachCheck.map(_.truncatedTo(SECONDS).toString.stripPrefix("PT")).take(18).mkString(", ")

    println(s"Elapsed time at each check : $elapsedTimesAtEachCheck")
  }

  it should "serialise to Dynamo" in {
    val ar = AvailabilityRecord(
      uri = uri"https://www.theguardian.com/foo",
      finalUriAfterRedirects = Some(uri"https://www.theguardian.com/bar"),
      uriResolvedOk = true,
      firstSeenInSitemap = Instant.parse("2022-11-02T16:41:37Z"),
      missing = Some(Instant.parse("2022-12-05T20:10:34Z")),
      found = None
    )
    val obj: DynamoObject = AvailabilityRecord.formatAvailabilityRecord.write(ar).asObject.value
    obj("uri").value shouldBe DynamoValue.fromString("https://www.theguardian.com/foo")
    obj("finalUriAfterRedirects").value shouldBe DynamoValue.fromString("https://www.theguardian.com/bar")
    obj("uriResolvedOk").value shouldBe DynamoValue.fromBoolean(true)
    obj("firstSeenInSitemap").value shouldBe DynamoValue.fromString("2022-11-02T16:41:37Z")
    obj("missing").value shouldBe DynamoValue.fromString("2022-12-05T20:10:34Z")
    obj("found") shouldBe None
    obj(FirstSeenInSitemapDateIndexKey).value shouldBe DynamoValue.fromString("2022-11-02Z")
  }

  it should "Create an AvailabilityRecord instance and the finalUriAfterRedirects property is correct" in {
    val resolved: Resolution.Resolved = Resolution.Resolved(RedirectPath(Seq(
      uri"https://www.bbc.co.uk/news/uk-politics-63534039",
      uri"https://www.bbc.co.uk/news/av/uk-politics-63534039"
    )), Conclusion.Ok)

    val availabilityRecord: AvailabilityRecord = AvailabilityRecord(resolved, Instant.parse("2022-12-05T20:10:34Z"))

    availabilityRecord.uri shouldBe uri"https://www.bbc.co.uk/news/uk-politics-63534039"
    availabilityRecord.finalUriAfterRedirects.value shouldBe uri"https://www.bbc.co.uk/news/av/uk-politics-63534039"
    availabilityRecord.ultimateUri shouldBe uri"https://www.bbc.co.uk/news/av/uk-politics-63534039"
  }
}
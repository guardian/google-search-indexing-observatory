package ophan.google.indexing.observatory.model

import ophan.google.indexing.observatory.model.AvailabilityRecord.{DelayForFirstCheckAfterContentIsFirstSeenInSitemap, reasonableTimeBetweenChecksForContentAged}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Duration
import java.time.Duration.{ofHours, ofMinutes, ofSeconds}
import java.time.temporal.ChronoUnit.SECONDS
import scala.math.Ordering.Implicits._

class AvailabilityRecordTest extends AnyFlatSpec with Matchers {

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
}

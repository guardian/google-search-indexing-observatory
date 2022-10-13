package ophan.google.indexing.observatory.model

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scanamo.DynamoValue

import java.time.Instant
import scala.util.Success

class CheckReportTest extends AnyFlatSpec with Matchers with OptionValues {
  it should "Persist timestamps as strings" in {
    val checkReport = CheckReport(Instant.ofEpochMilli(0), accessGoogleIndex = Success(true))

    val updateExpression = checkReport.asUpdateExpression.value
    val values = updateExpression.dynamoValues
    values.values.head shouldBe DynamoValue.fromString("1970-01-01T00:00:00Z")
  }
}

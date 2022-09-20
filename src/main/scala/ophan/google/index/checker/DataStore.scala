package ophan.google.index.checker

import ophan.google.index.checker.DataStore.{scanamoAsync, table}
import ophan.google.index.checker.model.{AvailabilityRecord, CheckReport}
import org.scanamo._
import org.scanamo.generic.semiauto._
import org.scanamo.syntax._

import java.time.Instant
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit.SECONDS
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataStore {
  implicit val instantAsISO8601StringFormat: DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, DateTimeParseException](Instant.parse, _.truncatedTo(SECONDS).toString)

  implicit val formatAvailabilityRecord: DynamoFormat[AvailabilityRecord] = deriveDynamoFormat

  val table = Table[AvailabilityRecord]("roberto-test2-google-search-index-checks")

  val scanamoAsync = ScanamoAsync(AWS.dynamoDb)

}

case class DataStore() {
  def fetchExistingRecordsFor(capiIds: Set[String]): Future[Map[String,AvailabilityRecord]] = (scanamoAsync.exec(
    table.getAll("capiId" in capiIds)
  ).map(_.flatMap(_.toOption).map(record => record.capiId -> record).toMap))

  def update(capiId: String, checkReport: CheckReport): Future[Option[AvailabilityRecord]] = {
    import DataStore.instantAsISO8601StringFormat

    checkReport.accessGoogleIndex.fold(_ => Future.successful(None), found =>
      scanamoAsync.exec(
        table.update("capiId" === capiId, append(if (found) "found" else "missing", checkReport.time))
      ).map(_.toOption)
    )
  }

}

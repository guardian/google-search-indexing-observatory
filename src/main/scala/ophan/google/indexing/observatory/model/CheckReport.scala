package ophan.google.indexing.observatory.model

import AvailabilityRecord.Field
import ophan.google.indexing.observatory.model.AvailabilityRecord.Field
import org.scanamo.syntax._
import org.scanamo.update.UpdateExpression

import java.time.Instant
import scala.util.Try

case class CheckReport(time: Instant, accessGoogleIndex: Try[Boolean]) {
  import AvailabilityRecord.instantAsISO8601StringFormat
  val asUpdateExpression: Option[UpdateExpression] =
    accessGoogleIndex.toOption.map(found => set(Field.timestampFor(found), time))
}

package ophan.google.index.observatory.model

import ophan.google.index.observatory.model.AvailabilityRecord.Field
import org.scanamo.syntax._
import org.scanamo.update.UpdateExpression

import java.time.Instant
import scala.util.Try

case class CheckReport(time: Instant, accessGoogleIndex: Try[Boolean]) {
  val asUpdateExpression: Option[UpdateExpression] =
    accessGoogleIndex.toOption.map(found => set(Field.timestampFor(found), time))
}

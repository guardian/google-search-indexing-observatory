package ophan.google.indexing.observatory

import DataStore.{scanamoAsync, table}
import ophan.google.indexing.observatory.model.AvailabilityRecord._
import ophan.google.indexing.observatory.model.{AvailabilityRecord, CheckReport}
import org.scanamo._
import org.scanamo.syntax._
import org.scanamo.update.UpdateExpression

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataStore {

  val table: Table[AvailabilityRecord] = Table("observatory-table") // TODO, read from paramstore?

  val scanamoAsync: ScanamoAsync = ScanamoAsync(AWS.dynamoDb)

}

case class DataStore() {
  def fetchExistingRecordsFor(uris: Set[URI]): Future[Map[URI,AvailabilityRecord]] = scanamoAsync.exec(
    table.getAll(Field.Uri in uris)
  ).map(_.flatMap(_.toOption).map(record => record.uri -> record).toMap)

  def update(uri: URI, hasExistingRecord: Boolean, checkReport: CheckReport): Future[Option[AvailabilityRecord]] = {
    val updateExpressionOpt: Option[UpdateExpression] = (checkReport.asUpdateExpression.toSeq ++
      Option.when(!hasExistingRecord)(set(Field.FirstSeenInSitemap, checkReport.time))).reduceOption(_ and _)

    updateExpressionOpt.fold[Future[Option[AvailabilityRecord]]](Future.successful(None)) { updateExpression =>
      scanamoAsync.exec(
        table.update(Field.Uri === uri, updateExpression)
      ).map(_.toOption)
    }
  }
}

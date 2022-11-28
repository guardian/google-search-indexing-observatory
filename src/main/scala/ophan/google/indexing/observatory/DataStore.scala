package ophan.google.indexing.observatory

import ophan.google.indexing.observatory.DataStore.{scanamoAsync, table}
import ophan.google.indexing.observatory.Resolution.Resolved
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.AvailabilityRecord.*
import ophan.google.indexing.observatory.model.{AvailabilityRecord, CheckReport}
import org.scanamo.*
import org.scanamo.syntax.*

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataStore {

  val table: Table[AvailabilityRecord] = Table("ophan-PROD-google-search-indexing-observatory-TableCD117FA1-D1EFSGIPXO63") // TODO, read from paramstore?

  val scanamoAsync: ScanamoAsync = ScanamoAsync(AWS.dynamoDb)

}

case class DataStore() extends Logging {
  def fetchExistingRecordsFor(uris: Set[URI]): Future[Set[AvailabilityRecord]] = scanamoAsync.exec(
    table.getAll(Field.Uri in uris)
  ).map(_.flatMap(_.toOption))


  /**
   * When we initially store an availability record in the DynamoDB table, we don't store anything about its
   * availability, just its URL, whether it redirects, and the time we first have seen this url.
   */
  def storeNewRecordsFor(sitemapDownload: SitemapDownload, urisNotSeenBefore: Set[URI]): Future[Unit] = {
    if (urisNotSeenBefore.isEmpty) Future.successful(()) else {
      logger.info(Map(
        "site" -> sitemapDownload.site.url,
        "sitemap.uris.all" -> sitemapDownload.allUris.size,
      ) ++ contextSampleOf("sitemap.uris.urisNotSeenBefore", urisNotSeenBefore),
        s"Storing new uris for ${sitemapDownload.site.url}")
      scanamoAsync.exec(
        table.putAll(urisNotSeenBefore.map { uri =>
          val skippedResolution: Resolved = Resolution.Resolved(RedirectPath(Seq(uri)), false)
          AvailabilityRecord(skippedResolution, sitemapDownload.timestamp)
        })
      )
    }
  }

  def update(uri: URI, checkReport: CheckReport): Future[Option[AvailabilityRecord]] = {
    checkReport.asUpdateExpression.fold(Future.successful(Option.empty[AvailabilityRecord])) { updateExpression =>
      scanamoAsync.exec(
        table.update(Field.Uri === uri, updateExpression)
      ).map(_.toOption)
    }
  }
}

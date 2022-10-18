package ophan.google.indexing.observatory

import DataStore.{scanamoAsync, table}
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.AvailabilityRecord._
import ophan.google.indexing.observatory.model.{AvailabilityRecord, CheckReport}
import org.scanamo._
import org.scanamo.syntax._
import org.scanamo.update.UpdateExpression

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataStore {

  val table: Table[AvailabilityRecord] = Table("ophan-PROD-google-search-indexing-observatory-TableCD117FA1-D1EFSGIPXO63") // TODO, read from paramstore?

  val scanamoAsync: ScanamoAsync = ScanamoAsync(AWS.dynamoDb)

}

case class DataStore() extends Logging {
  def fetchExistingRecordsFor(uris: Set[URI]): Future[Map[URI,AvailabilityRecord]] = scanamoAsync.exec(
    table.getAll(Field.Uri in uris)
  ).map(_.flatMap(_.toOption).map(record => record.uri -> record).toMap)

  def storeNewRecordsFor(sitemapDownload: SitemapDownload, alreadyKnownUris: Set[URI]): Future[Unit] = {
    val urisNotSeenBefore = sitemapDownload.allUris -- alreadyKnownUris
    println(s"urisNotSeenBefore=$urisNotSeenBefore site=${sitemapDownload.site}")
    if (urisNotSeenBefore.isEmpty) Future.successful(()) else {
      logger.info(Map(
        "site" -> sitemapDownload.site.url,
        "site.sitemap.uris.all" -> sitemapDownload.allUris.size,
        "site.sitemap.uris.old" -> alreadyKnownUris.size,
        "site.sitemap.uris.new" -> urisNotSeenBefore.size
      ), s"Storing ${urisNotSeenBefore.size} new uris for ${sitemapDownload.site.url}")
      scanamoAsync.exec(
        table.putAll(urisNotSeenBefore.map(uri => AvailabilityRecord(uri, sitemapDownload.timestamp)))
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

package ophan.google.indexing.observatory

import cats.implicits._
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.{AvailabilityRecord, Site}

import java.net.URI
import java.time.Clock.systemUTC
import java.time.{Clock, Duration}
import java.time.temporal.ChronoUnit.MINUTES
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.math.Ordering.Implicits._

case class AvailabilityUpdaterService(
  dataStore: DataStore,
  googleSearchService: GoogleSearchService
)(implicit
  ec: ExecutionContext
) extends Logging {

  val MaxAgeOfUriToScan: Duration = Duration.ofHours(4)

  def availabilityFor(sitemapDownload: SitemapDownload): Future[Map[URI, AvailabilityRecord]] = {
    for {
      existingRecordsByUri <- dataStore.fetchExistingRecordsFor(sitemapDownload.allUris)
      storageF = dataStore.storeNewRecordsFor(sitemapDownload, existingRecordsByUri.keySet)
      updatedAvailabilityReports <- checkMostUrgentOf(existingRecordsByUri, sitemapDownload.site)
      _ <- storageF // ...make sure storing new records has completed before we terminate
    } yield {
//      val unchangedRecordsForContentThatIsKnownToBeFine: Map[URI, AvailabilityRecord] = {
//        val urisOfContentThatIsKnownToBeFine = existingRecordsThatDoNotNeedCheckingRightNow.map(_.uri)
//        existingRecordsByUri.view.filterKeys(urisOfContentThatIsKnownToBeFine)
//      }.toMap
      updatedAvailabilityReports
//      unchangedRecordsForContentThatIsKnownToBeFine ++ updatedAvailabilityReports
    }
  }

  def checkMostUrgentOf(
    existingRecordsByURI: Map[URI, AvailabilityRecord],
    site: Site
  )(implicit clock: Clock = systemUTC): Future[Map[URI, AvailabilityRecord]] = {
    val earliestSitemapTimeToScan = clock.instant().minus(MaxAgeOfUriToScan)
    val existingRecords = existingRecordsByURI.values.filter(_.firstSeenInSitemap > earliestSitemapTimeToScan).toSet
    val currentlyRecordedMissing = existingRecords.filter(_.currentlyRecordedMissing)
    logger.info(Map(
      "site" -> site.url,
      "availabilityRecords.existing" -> existingRecords.size,
      "availabilityRecords.recordingMissingUris.count" -> currentlyRecordedMissing.size,
      "availabilityRecords.recordingMissingUris.sample" -> currentlyRecordedMissing.toSeq.sortBy(_.missing).take(3).map(_.uri).asJava,
    ), s"Checking Google index...")
    existingRecords.map(_.firstSeenInSitemap).minOption.fold(
      Future.successful(Map.empty[URI, AvailabilityRecord])
    ) { earliestMomentOfSitemap =>
      val timeThreshold = earliestMomentOfSitemap.plus(1, MINUTES)
      val (neverScannedRecord, missingTimeAndRecord) = existingRecords
        .filter(record => record.firstSeenInSitemap > timeThreshold && record.needsCheckingNow())
        .map(record => record.missing.toRight(record).map(_ -> record)).toSeq.separate

      val mostUrgentUrisAlreadyRecordedAsMissingFromGoogle = missingTimeAndRecord.sortBy(_._1).map(_._2.uri).take(10)

      val urisMostRecentlyArrivedInSitemapNotYetScanned =
        neverScannedRecord.sortBy(_.firstSeenInSitemap).reverse.map(_.uri)

      val mostUrgentUris =
        (mostUrgentUrisAlreadyRecordedAsMissingFromGoogle ++ urisMostRecentlyArrivedInSitemapNotYetScanned).take(20)

      logger.info(Map(
        "site" -> site.url,
        "uris.mostUrgentUrisAlreadyRecordedAsMissingFromGoogle" -> mostUrgentUrisAlreadyRecordedAsMissingFromGoogle.size,
        "uris.mostUrgentUris" -> mostUrgentUris.size,
      ), s"Checking Google index... mostUrgentUrisAlreadyRecordedAsMissingFromGoogle=${mostUrgentUrisAlreadyRecordedAsMissingFromGoogle.mkString(", ")}")

      Future.traverse(mostUrgentUris) { uri =>
        for {
          checkReport <- googleSearchService.contentAvailabilityInGoogleIndex(uri, site)
          updatedAvailabilityRecord <- dataStore.update(uri, checkReport)
        } yield updatedAvailabilityRecord
      }.map(_.flatten.map(record => record.uri -> record).toMap)
    }
  }

  

}

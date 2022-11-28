package ophan.google.indexing.observatory

import cats.implicits.*
import ophan.google.indexing.observatory.AvailabilityUpdaterService.mostUrgent
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.{AvailabilityRecord, Site}

import java.net.URI
import java.time.Clock.systemUTC
import java.time.temporal.ChronoUnit.MINUTES
import java.time.{Clock, Duration}
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Ordering.Implicits.*

case class AvailabilityUpdaterService(
  redirectResolver: RedirectResolver,
  dataStore: DataStore,
  googleSearchService: GoogleSearchService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def availabilityFor(sitemapDownload: SitemapDownload): Future[Set[AvailabilityRecord]] = {
    for {
      existingRecords <- dataStore.fetchExistingRecordsFor(sitemapDownload.allUris)
      // New uris must have their redirects resolved & be checked to see that they return HTTP 200 OK, with that info stored in new records
      // Old uris must be considered for Google Search checks
      storageF = processNewUris(sitemapDownload, existingRecords)
      updatedAvailabilityReports <- checkMostUrgentOf(existingRecords)(using sitemapDownload.site)
      _ <- storageF // ...make sure storing new records has completed before we terminate
    } yield updatedAvailabilityReports
  }

  // New uris must have their redirects resolved & be checked to see that they return HTTP 200 OK, with that info stored in new records
  def processNewUris(sitemapDownload: SitemapDownload, excludingAlreadyExistingRecords: Set[AvailabilityRecord]): Future[_] = {
    val urisNotSeenBefore = sitemapDownload.allUris -- excludingAlreadyExistingRecords.map(_.uri)
    for {
      redirectResolutionsForUrisNotSeenBefore <- Future.traverse(urisNotSeenBefore)(redirectResolver.resolve)
      _ <- dataStore.storeNewRecordsFor(sitemapDownload, redirectResolutionsForUrisNotSeenBefore.collect {
        case r: Resolution.Resolved => r
      })
    } yield ()
  }

  def checkMostUrgentOf(existingRecords: Set[AvailabilityRecord])(using site: Site): Future[Set[AvailabilityRecord]] =
    checkAndUpdate(mostUrgent(existingRecords))

  private def checkAndUpdate(records: Seq[AvailabilityRecord])(using site: Site): Future[Set[AvailabilityRecord]] = for {
    updatedAvailabilityRecords <- Future.traverse(records) { availabilityRecord =>
      for {
        checkReport <- googleSearchService.contentAvailabilityInGoogleIndex(availabilityRecord.ultimateUri, site)
        updatedAvailabilityRecord <- dataStore.update(availabilityRecord.uri, checkReport)
      } yield updatedAvailabilityRecord
    }
  } yield {
    val updatedRecords = updatedAvailabilityRecords.flatten
    logger.info(Map("site" -> site.url)
      ++ contextSampleOf("uris.search.checked", records.map(_.uri))
      ++ contextSampleOf("uris.search.found", updatedRecords.filter(_.contentHasBeenFound).map(_.uri)),
      s"Checked Google for ${records.size} urls")
    updatedRecords.toSet
  }
}

object AvailabilityUpdaterService extends Logging {

  val MaxAgeOfUriToScan: Duration = Duration.ofHours(4)

  def reduceLoadByDiscardingOldestContent(existingRecordsForUrlsInSitemap: Set[AvailabilityRecord])(using clock: Clock = systemUTC()): Set[AvailabilityRecord] = {
    if (existingRecordsForUrlsInSitemap.size < 10) existingRecordsForUrlsInSitemap
    else {
      val recencyThreshold = clock.instant().minus(MaxAgeOfUriToScan) // don't scan really old stuff

      // don't scan THE VERY EARLIEST items - who knows how long they had been published before we turned on scanning?
      val earliestItemsThreshold =
        existingRecordsForUrlsInSitemap.map(_.firstSeenInSitemap).minOption.map(_.plus(1, MINUTES))

      val timeThreshold = (Set(recencyThreshold) ++ earliestItemsThreshold).max

      existingRecordsForUrlsInSitemap.filter(_.firstSeenInSitemap > timeThreshold)
    }
  }

  def mostUrgent(existingRecords: Set[AvailabilityRecord])(using site: Site): Seq[AvailabilityRecord] = {
    val recordsNeedingCheck = reduceLoadByDiscardingOldestContent(existingRecords).filter(_.needsCheckingNow())

    val (neverScannedRecords, missingTimesAndRecords) =
      recordsNeedingCheck.map(record => record.missing.toRight(record).map(_ -> record)).toSeq.separate

    val mostUrgentUrisAlreadyRecordedAsMissingFromGoogle = missingTimesAndRecords.sortBy(_._1).map(_._2)

    val urisMostRecentlyArrivedInSitemapNotYetScanned = neverScannedRecords.sortBy(_.firstSeenInSitemap).reverse

    val recordsToCheck =
      (mostUrgentUrisAlreadyRecordedAsMissingFromGoogle.take(5) ++ urisMostRecentlyArrivedInSitemapNotYetScanned).take(5)

    logger.info(Map(
      "site" -> site.url,
    ) ++ contextSampleOf("uris.existingRecords", existingRecords.map(_.uri))
      ++ contextSampleOf("uris.recordsNeedingCheck", recordsNeedingCheck.map(_.uri))
      ++ contextSampleOf("uris.mostUrgent.alreadyRecordedAsMissingFromGoogle", mostUrgentUrisAlreadyRecordedAsMissingFromGoogle.map(_.uri))
      ++ contextSampleOf("uris.mostUrgent.selectedForCheck", recordsToCheck.map(_.uri)),
      s"Identified most urgent records")
    recordsToCheck
  }
}
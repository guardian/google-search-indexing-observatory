package ophan.google.indexing.observatory

import com.madgag.scala.collection.decorators.MapDecorator
import ophan.google.indexing.observatory.Credentials.logger
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.{AvailabilityRecord, ContentSummary, Site}

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

case class AvailabilityUpdaterService(
  dataStore: DataStore,
  googleSearchService: GoogleSearchService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def availabilityFor(uris: Set[URI], site: Site): Future[Map[URI, AvailabilityRecord]] = {
    for {
      existingRecordsByUri <- dataStore.fetchExistingRecordsFor(uris)
      (existingRecordsThatNeedCheckingNow, existingRecordsThatDoNotNeedCheckingRightNow) =
        existingRecordsByUri.values.toSet.partition(_.needsCheckingNow())
      urisForWhichThereIsNoExistingRecord = uris -- existingRecordsByUri.keySet
      updatedAvailabilityReports <-
        check(urisForWhichThereIsNoExistingRecord, existingRecordsThatNeedCheckingNow, site)
    } yield {
      println(s"There are ${existingRecordsByUri.size}/${uris.size} existing availability records for content items")
      val unchangedRecordsForContentThatIsKnownToBeFine: Map[URI, AvailabilityRecord] = {
        val urisOfContentThatIsKnownToBeFine = existingRecordsThatDoNotNeedCheckingRightNow.map(_.uri)
        existingRecordsByUri.view.filterKeys(urisOfContentThatIsKnownToBeFine)
      }.toMap

      unchangedRecordsForContentThatIsKnownToBeFine ++ updatedAvailabilityReports
    }
  }

  def check(
    urisForWhichThereIsNoExistingRecord: Set[URI],
    existingRecordsThatNeedCheckingNow: Set[AvailabilityRecord],
    site: Site
  ): Future[Map[URI, AvailabilityRecord]] = {
    require(existingRecordsThatNeedCheckingNow.map(_.uri).intersect(urisForWhichThereIsNoExistingRecord).isEmpty)

    val combined: Set[(URI, Boolean)] =
      urisForWhichThereIsNoExistingRecord.map(_ -> false) ++ existingRecordsThatNeedCheckingNow.map(_.uri -> true)

    val throttledList = combined.take(10)

    logger.info(Map(
      "site" -> site.url,
      "uris.needingChecking" -> combined.size,
      "uris.toAttempt" -> throttledList.size,
    ), s"Checking Google index for ${throttledList.size}/${combined.size} sitemap items")

    Future.traverse(throttledList) { case (uri, hasExistingRecord) =>
      for {
        checkReport <- googleSearchService.contentAvailabilityInGoogleIndex(uri, site)
        updatedAvailabilityRecord <- dataStore.update(uri, hasExistingRecord, checkReport)
      } yield updatedAvailabilityRecord
    }.map(_.flatten.map(record => record.uri -> record).toMap)
  }

  

}

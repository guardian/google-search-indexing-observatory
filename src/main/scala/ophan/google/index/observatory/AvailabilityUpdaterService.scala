package ophan.google.index.observatory

import com.madgag.scala.collection.decorators.MapDecorator
import ophan.google.index.observatory.model.{AvailabilityRecord, ContentAvailabilityInGoogleIndex, ContentSummary, Site}

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

case class AvailabilityUpdaterService(
  dataStore: DataStore,
  googleSearchService: GoogleSearchService
)(implicit
  ec: ExecutionContext
) {

  def availabilityFor(uris: Set[URI], site: Site): Future[Map[String, ContentAvailabilityInGoogleIndex]] = {
    for {
      existingRecordsByUri <- dataStore.fetchExistingRecordsFor(uris)
      (existingRecordsThatNeedCheckingNow, existingRecordsThatDoNotNeedCheckingRightNow) =
        existingRecordsByUri.values.toSet.partition(_.needsCheckingNow())
      urisForWhichThereIsNoExistingRecord = uris -- existingRecordsByUri.keySet
      updatedAvailabilityReports <-
        check(urisForWhichThereIsNoExistingRecord, existingRecordsThatNeedCheckingNow)
    } yield {
      println(s"There are ${existingRecordsByUri.size}/${uris.size} existing availability records for content items")
      val unchangedRecordsForContentThatIsKnownToBeFine: Map[String, AvailabilityRecord] = {
        val idsOfContentThatIsKnownToBeFine = existingRecordsThatDoNotNeedCheckingRightNow.map(_.id)
        existingRecordsByUri.view.filterKeys(idsOfContentThatIsKnownToBeFine)
      }.toMap

      (unchangedRecordsForContentThatIsKnownToBeFine ++ updatedAvailabilityReports).mapV { record =>
        ContentAvailabilityInGoogleIndex(contentSummariesById(record.capiId), record)
      }
    }
  }

  def check(
    urisForWhichThereIsNoExistingRecord: Set[URI],
    existingRecordsThatNeedCheckingNow: Set[AvailabilityRecord]
  ): Future[Map[URI, AvailabilityRecord]] = {
    require(existingRecordsThatNeedCheckingNow.map(_.uri).intersect(urisForWhichThereIsNoExistingRecord).isEmpty)

    println(s"Checking Google index for ${contentSummaries.size} content items")
    Future.traverse(contentSummaries) { content =>
      for {
        checkReport <- googleSearchService.contentAvailabilityInGoogleIndex(content)
        updatedAvailabilityRecord <- dataStore.update(content.id, checkReport)
      } yield updatedAvailabilityRecord
    }.map(_.flatten.map(record => record.capiId -> record).toMap)
    val successfulIndexStateChecks = checkReports.values.count(_.accessGoogleIndex.isSuccess)
    println(s"Search Index checks: $successfulIndexStateChecks/${contentThatNeedsCheckingNow.size} accessed Google's API without error")
  }

  

}

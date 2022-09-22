package ophan.google.index.checker

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.gu.contentapi.client.GuardianContentClient
import ophan.google.index.checker.Credentials.fetchKeyFromParameterStore
import ophan.google.index.checker.logging.Logging
import ophan.google.index.checker.model.{AvailabilityRecord, ContentAvailabilityInGoogleIndex, ContentSummary}

import java.time.Clock
import java.time.Clock.systemUTC
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Lambda extends Logging {

  val recentContentService: RecentContentService = {
    val capiKey = fetchKeyFromParameterStore("CAPI/ApiKey")
    new RecentContentService(new GuardianContentClient(capiKey))
  }

  val googleSearchService: GoogleSearchService = {
    val apiKey = fetchKeyFromParameterStore("Google/CustomSearch/ApiKeyNotEncrypted")
    new GoogleSearchService(apiKey)
  }

  val dataStore = new DataStore()


  // content that we have no 'found' record for - AND have not checked too often/recently
  def contentThatNeedsCheckingNowGiven(
    existingRecordsByCapiId: Map[String,AvailabilityRecord]
  )(content: ContentSummary)(implicit clock:Clock = systemUTC): Boolean =
    existingRecordsByCapiId.get(content.id).forall(content.shouldBeCheckedNowGivenExisting)

  /*
       * Logic handler
       */
  def go(): Unit = {
    val eventual = for {
      contentSummaries <- recentContentService.fetchRecentContent()
      availability <- availabilityFor(contentSummaries.toSet)
    } yield {
      println("Mostly done!")
      val allWorryinglyAbsentContent = availability.values.filter(_.contentIsCurrentlyWorryinglyAbsentFromGoogle())
      //println(f"Missing from Google index: ${100f*allWorryinglyAbsentContent.size/successfulIndexStateChecks}%.1f%%")
      for {
        worryinglyAbsentContent <- allWorryinglyAbsentContent.toSeq.sortBy(_.contentSummary.firstPublished)
      } {
        val content = worryinglyAbsentContent.contentSummary
        println(s"${content.timeSinceUrlWentPublic().toMinutes}mins ${content.ophanUrl}\n${content.googleSearchUiUrl}\n")
      }
      // checkReports
    }

    Await.result(eventual , 10.seconds)
  }

  def availabilityFor(contentSummaries: Set[ContentSummary]): Future[Map[String, ContentAvailabilityInGoogleIndex]] = {
    val contentSummariesById: Map[String, ContentSummary] = contentSummaries.map(cs => cs.id -> cs).toMap
    for {
      existingRecordsByCapiId <- dataStore.fetchExistingRecordsFor(contentSummariesById.keySet)
      (contentThatNeedsCheckingNow, contentThatDoesNotNeedCheckingRightNow) =
        contentSummaries.partition(contentThatNeedsCheckingNowGiven(existingRecordsByCapiId))
      updatedAvailabilityReports <- check(contentThatNeedsCheckingNow)
    } yield {
      println(s"There are ${existingRecordsByCapiId.size}/${contentSummaries.size} existing availability records for content items")
      val unchangedRecordsForContentThatIsKnownToBeFine: Map[String, AvailabilityRecord] = {
        val idsOfContentThatIsKnownToBeFine = contentThatDoesNotNeedCheckingRightNow.map(_.id)
        existingRecordsByCapiId.view.filterKeys(idsOfContentThatIsKnownToBeFine)
      }.toMap

      (unchangedRecordsForContentThatIsKnownToBeFine ++ updatedAvailabilityReports).view.mapValues { record =>
        ContentAvailabilityInGoogleIndex(contentSummariesById(record.capiId), record)
      }.toMap
    }
  }

  def check(contentSummaries: Set[ContentSummary]): Future[Map[String, AvailabilityRecord]] = {
    println(s"Checking Google index for ${contentSummaries.size} content items")
    Future.traverse(contentSummaries) { content =>
      for {
        checkReport <- googleSearchService.contentAvailabilityInGoogleIndex(content)
        updatedAvailabilityRecord <- dataStore.update(content.id, checkReport)
      } yield updatedAvailabilityRecord
    }.map(_.flatten.map(record => record.capiId -> record).toMap)
//    val successfulIndexStateChecks = checkReports.values.count(_.accessGoogleIndex.isSuccess)
//    println(s"Search Index checks: $successfulIndexStateChecks/${contentThatNeedsCheckingNow.size} accessed Google's API without error")

  }



  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: ScheduledEvent, context: Context): Unit = {
    go()
  }

}

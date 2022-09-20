package ophan.google.index.checker

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.customsearch.v1.CustomSearchAPI
import com.google.api.services.customsearch.v1.model.{Result, Search}
import com.gu.contentapi.client.model.SearchQuery
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.{ContentApiClient, GuardianContentClient}
import ophan.google.index.checker.logging.Logging
import ophan.google.index.checker.model.{AvailabilityRecord, CheckReport, ContentAvailabilityInGoogleIndex, ContentSummary}
import org.scanamo.ScanamoAsync
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Clock.systemUTC
import java.time.{Clock, Instant}
import java.util
import scala.collection.{MapView, mutable}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Lambda extends Logging {

  val recentContentService: RecentContentService = {
    val capiKey: String = fetchKeyFromParameterStore("/Ophan/dashboard-es7/CODE/ContentApiKey")
    new RecentContentService(new GuardianContentClient(capiKey))
  }

  val googleSearchService: GoogleSearchService = {
    val apiKey = fetchKeyFromParameterStore("/Ophan/Google/CustomSearch/ApiKey")
    new GoogleSearchService(apiKey)
  }

  val dataStore = new DataStore()

  private def fetchKeyFromParameterStore(value: String): String =
    AWS.SSM.getParameter(_.withDecryption(true).name(value)).parameter.value

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
        worryinglyAbsentContent <- allWorryinglyAbsentContent
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

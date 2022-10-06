package ophan.google.index.observatory

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import ophan.google.index.observatory.Credentials.fetchKeyFromParameterStore
import ophan.google.index.observatory.logging.Logging
import ophan.google.index.observatory.model.{AvailabilityRecord, ContentAvailabilityInGoogleIndex, ContentSummary, Site, Sites}

import java.time.Clock
import java.time.Clock.systemUTC
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.madgag.scala.collection.decorators._

import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration.ofSeconds
import scala.jdk.FutureConverters._




object Lambda extends Logging {

  val sitemapDownloader = new SitemapDownloader()

  val googleSearchService: GoogleSearchService = {
    val apiKey = fetchKeyFromParameterStore("Google/CustomSearch/ApiKeyNotEncrypted")
    new GoogleSearchService(apiKey)
  }

  val dataStore = new DataStore()

  // content that we have no 'found' record for - AND have not checked too often/recently
//  def contentThatNeedsCheckingNowGiven(
//    existingRecordsByCapiId: Map[String,AvailabilityRecord]
//  )(content: ContentSummary)(implicit clock:Clock = systemUTC): Boolean =
//    existingRecordsByCapiId.get(content.id).forall(content.shouldBeCheckedNowGivenExisting)

  /*
       * Logic handler
       */
  def go(): Unit = {
    Future.traverse(Sites.All) { site =>
      sitemapDownloader.fetchSitemapEntriesFor(site)
    }


//    val eventual = for {
//      contentSummaries <- recentContentService.fetchRecentContent()
//      availability <- availabilityFor(contentSummaries.toSet)
//    } yield {
//      println("Mostly done!")
//      val allWorryinglyAbsentContent = availability.values.filter(_.contentIsCurrentlyWorryinglyAbsentFromGoogle())
//      //println(f"Missing from Google index: ${100f*allWorryinglyAbsentContent.size/successfulIndexStateChecks}%.1f%%")
//      for {
//        worryinglyAbsentContent <- allWorryinglyAbsentContent.toSeq.sortBy(_.contentSummary.firstPublished)
//      } {
//        val content = worryinglyAbsentContent.contentSummary
//        println(s"${content.timeSinceUrlWentPublic().toMinutes}mins ${content.ophanUrl}\n${content.googleSearchUiUrl}\n")
//      }
//      // checkReports
//    }

//    Await.result(eventual , 10.seconds)
  }




  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: ScheduledEvent, context: Context): Unit = {
    go()
  }

}

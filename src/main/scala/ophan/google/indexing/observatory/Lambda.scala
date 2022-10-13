package ophan.google.indexing.observatory

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import Credentials.fetchKeyFromParameterStore

import java.time.Clock
import java.time.Clock.systemUTC
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.madgag.scala.collection.decorators._
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.{AvailabilityRecord, ContentSummary, Site, Sites}

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration.ofSeconds
import scala.jdk.FutureConverters._
import cats.data.EitherT
import cats.implicits._




object Lambda extends Logging {

  val sitemapDownloader = new SitemapDownloader()

  val googleSearchService: GoogleSearchService = {
    val apiKey = fetchKeyFromParameterStore("Google/CustomSearch/ApiKey")
    new GoogleSearchService(apiKey)
  }

  val dataStore = new DataStore()

  val availabilityUpdaterService = new AvailabilityUpdaterService(dataStore, googleSearchService)

  // content that we have no 'found' record for - AND have not checked too often/recently
//  def contentThatNeedsCheckingNowGiven(
//    existingRecordsByCapiId: Map[String,AvailabilityRecord]
//  )(content: ContentSummary)(implicit clock:Clock = systemUTC): Boolean =
//    existingRecordsByCapiId.get(content.id).forall(content.shouldBeCheckedNowGivenExisting)

  /*
   * Logic handler
   */
  def go(): Unit = {
    val eventual = Future.traverse(Sites.All) { site =>
      println(s"Handing site ${site.url}")

      (for {
        sitemapEntries <- sitemapDownloader.fetchSitemapEntriesFor(site).attemptT
        updatedAvailabilityRecords <-
          EitherT.right[Throwable](availabilityUpdaterService.availabilityFor(sitemapEntries, site))
      } yield {
        println(s"Completed site ${site.url}")
        updatedAvailabilityRecords
      }).value
    }

    Await.result(eventual, 40.seconds)
    println("Everything complete")



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

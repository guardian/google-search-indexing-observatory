package ophan.google.indexing.observatory

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import Credentials.fetchKeyFromParameterStore

import java.time.Clock
import java.time.Clock.systemUTC
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import com.madgag.scala.collection.decorators.*
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.{AvailabilityRecord, Site, Sites}

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration.ofSeconds
import scala.jdk.FutureConverters.*
import cats.data.EitherT
import cats.implicits.*

import scala.util.{Failure, Success}


object Lambda extends Logging {

  val sitemapDownloader = new SitemapDownloader()

  val googleSearchService: GoogleSearchService = {
    val apiKey = fetchKeyFromParameterStore("Google/CustomSearch/ApiKey")
    new GoogleSearchService(apiKey)
  }

  val dataStore = new DataStore()

  private val redirectResolver = new RedirectResolver(RedirectFollower)
  val availabilityUpdaterService = new AvailabilityUpdaterService(redirectResolver, dataStore, googleSearchService)

  /*
   * Logic handler
   */
  def go(): Unit = {
    val eventual = Future.traverse(Sites.All) { site =>
      println(s"Handing site ${site.url}")

      val sitemapDownloadF = sitemapDownloader.fetchSitemapEntriesFor(site).attemptT
      sitemapDownloadF.value.onComplete {
        case Success(Left(e)) => logger.error("Problem getting sitemaps", e)
        case _ => ()
      }
      (for {
        sitemapDownload <- sitemapDownloadF
        updatedAvailabilityRecords <-
          EitherT.right[Throwable](availabilityUpdaterService.availabilityFor(sitemapDownload))
      } yield {
        println(s"Completed site ${site.url}")
        updatedAvailabilityRecords
      }).value
    }

    Await.result(eventual, 40.seconds)
    println("Everything complete")

    Await.ready({
      val eventualResolution = redirectResolver.resolve(URI.create("https://www.bbc.co.uk/news/uk-politics-63534039"))
      eventualResolution.foreach { resolution =>
        logger.info(s"BBC url resolved to $resolution")
      }
      eventualResolution
    }, 10.seconds)
  }

  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: ScheduledEvent, context: Context): Unit = {
    go()
  }
}

package ophan.google.indexing.observatory

import cats.data.EitherT
import cats.implicits.*
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.gu.http.redirect.resolver.UrlResolver
import ophan.google.indexing.observatory.Credentials.fetchKeyFromParameterStore
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.Sites

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.util.Success


object Lambda extends Logging {

  val sitemapDownloader = new SitemapDownloader()

  val googleSearchService: GoogleSearchService = {
    val apiKey = fetchKeyFromParameterStore("Google/VertexAISearch/ApiKeyNotEncrypted")
    new GoogleSearchService(apiKey, "ophan-reborn-2017", "global")
  }

  val dataStore = new DataStore()

  private val redirectResolver = new UrlResolver(RedirectFollower)
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
  }

  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: ScheduledEvent, context: Context): Unit = {
    go()
  }
}

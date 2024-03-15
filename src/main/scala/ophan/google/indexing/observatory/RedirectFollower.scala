package ophan.google.indexing.observatory

import com.github.blemale.scaffeine.{AsyncLoadingCache, Scaffeine}
import com.gu.http.redirect.resolver.UrlResponseFetcher
import com.gu.http.redirect.resolver.UrlResponseFetcher.HttpResponseSummary
import com.gu.http.redirect.resolver.UrlResponseFetcher.HttpResponseSummary.LocationHeader
import okhttp3.{Headers, OkHttpClient, Request}
import ophan.google.indexing.observatory.logging.Logging

import java.net.URI
import java.time.Duration
import java.time.Duration.ofSeconds
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.Try

object RedirectFollower extends UrlResponseFetcher with Logging {

  extension (headers: Headers) def location: Option[LocationHeader] = Option(headers.get("Location")).map(LocationHeader.apply)

  private val httpClient: OkHttpClient =
    new OkHttpClient.Builder()
      .followRedirects(false)
      .followSslRedirects(false)
      .callTimeout(Duration.ofSeconds(5))
      .build()

  def requestFor(uri: URI): Request =
    new Request.Builder()
      .url(uri.toURL)
      .head()
      .header("User-Agent", "curl/7.54")
      .build();


  override def fetchResponseFor(uri: URI)(implicit ec: ExecutionContext): Future[HttpResponseSummary] = {
    Future {
      blocking {
        httpClient.newCall(requestFor(uri)).execute()
      }
    }.map {
      response =>
        val statusCode = response.code()
        val responseOk = statusCode == 200
        if (!responseOk) {
          logger.warn(Map(
            "redirect.resolution.uri" -> uri.toString,
            "redirect.resolution.statusCode" -> statusCode
          ), s"Bad status code $statusCode (not a redirect!) while trying to resolve '$uri'")
        }
        HttpResponseSummary(statusCode, response.headers.location)
      }
    }
}


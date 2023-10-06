package ophan.google.indexing.observatory

import com.github.blemale.scaffeine.{AsyncLoadingCache, Scaffeine}
import okhttp3.{Headers, OkHttpClient, Request}
import ophan.google.indexing.observatory.Resolution.{Resolved, Unresolved}
import ophan.google.indexing.observatory.logging.Logging

import java.net.URI
import java.time.Duration
import java.time.Duration.ofSeconds
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Future, blocking}
import scala.util.Try

case class RedirectPath(locations: Seq[URI]) {
  val originalUri: URI = locations.head
  val numRedirects: Int = locations.size - 1
  val doesRedirect: Boolean = numRedirects > 0

  val isLoop: Boolean = locations.dropRight(1).contains(locations.last)

  def adding(location: URI): RedirectPath = copy(locations :+ location)
}

enum Resolution {
  val redirectPath: RedirectPath

  case Resolved(redirectPath: RedirectPath, ok: Boolean)
  case Unresolved(redirectPath: RedirectPath)
}

type Resp = Either[Boolean,URI]

trait RedirectFollower {
  /**
   * Possible results:
   * * HTTP 200
   *
   *
   * Failure:
   * * Domain does not exist - www.doesnotexist123123.com
   * * Page is 404 - https://www.theguardian.com/uk/roberto-is-so-cool
   * * Some kind of transient network connection failure
   *
   * @return Future(Left(bool)) if the url did not redirect elsewhere - `true` if we got a HTTP 200 response
   */
  def follow(uri: URI): Future[Resp]
}



object RedirectFollower extends RedirectFollower with Logging {

  val HTTPRedirectStatusCodes: Set[Int] = Set(301, 302, 303, 307, 308)

  def redirectGivenBy(requestUri: URI, response: okhttp3.Response): Option[URI] =
    if (HTTPRedirectStatusCodes.contains(response.code())) getRedirectedURI(requestUri, response.headers()) else None

  // based on https://github.com/openjdk/jdk17/blob/4afbcaf55/src/java.net.http/share/classes/jdk/internal/net/http/RedirectFilter.java#L132
  private def getRedirectedURI(requestUri: URI, headers: Headers): Option[URI] =
    headers.location.flatMap { newLocation =>
      Try(URI.create(newLocation)).toOption
    }.map(requestUri.resolve) // redirect could be relative to original URL


  extension (headers: Headers) def location: Option[String] = Option(headers.get("Location"))

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

  def follow(uri: URI): Future[Resp] = Future {
    blocking {
      httpClient.newCall(requestFor(uri)).execute()
    }
  }.map {
    response => redirectGivenBy(uri, response).toRight {
      val statusCode = response.code()
      val responseOk = statusCode == 200
      if (!responseOk) {
        logger.warn(Map(
          "redirect.resolution.uri" -> uri.toString,
          "redirect.resolution.statusCode" -> statusCode
        ), s"Bad status code $statusCode (not a redirect!) while trying to resolve '$uri'")
      }
      responseOk
    }
  }.recover { exception =>
    logger.warn(Map(
      "redirect.resolution.uri" -> uri.toString
    ), s"Exception while trying to resolve '$uri'", exception)
    Left(false)
  }
}

class RedirectResolver(redirectFollower: RedirectFollower, val maxRedirects: Int = 5) {

  private val cache: AsyncLoadingCache[URI, Resp] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(5.minutes)
      .maximumSize(100000)
      .buildAsyncFuture(redirectFollower.follow)

  def resolve(uri: URI): Future[Resolution] = resolve(RedirectPath(Seq(uri)))

  def resolve(redirectPath: RedirectPath): Future[Resolution] =
    if (redirectPath.numRedirects >= maxRedirects || redirectPath.isLoop) Future.successful(Unresolved(redirectPath))
    else cache.get(redirectPath.locations.last).flatMap {
      resp => resp.fold(
        ok => Future.successful(Resolved(redirectPath, ok)),
        redirectUri => resolve(redirectPath.adding(redirectUri))
      )
    }

}

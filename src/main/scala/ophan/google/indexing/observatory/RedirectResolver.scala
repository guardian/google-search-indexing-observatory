package ophan.google.indexing.observatory

import com.github.blemale.scaffeine.{AsyncLoadingCache, Scaffeine}
import ophan.google.indexing.observatory.Resolution.{Resolved, Unresolved}

import java.net.http.*
import java.net.http.HttpClient.Redirect
import java.time.Duration
import java.time.Duration.ofSeconds
import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import scala.jdk.OptionConverters.*
import scala.util.Try
import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpRequest.BodyPublishers.noBody
import concurrent.duration.*
import scala.concurrent.Future
import scala.jdk.FutureConverters.*
import concurrent.ExecutionContext.Implicits.global

case class RedirectPath(locations: Seq[URI]) {
  val numRedirects: Int = locations.size - 1

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



object RedirectFollower extends RedirectFollower {

  val HTTPRedirectStatusCodes: Set[Int] = Set(301, 302, 303, 307, 308)

  def redirectGivenBy(requestUri: URI, response: HttpResponse[_]): Option[URI] =
    if (HTTPRedirectStatusCodes.contains(response.statusCode)) getRedirectedURI(requestUri, response.headers) else None

  // based on https://github.com/openjdk/jdk17/blob/4afbcaf55/src/java.net.http/share/classes/jdk/internal/net/http/RedirectFilter.java#L132
  private def getRedirectedURI(requestUri: URI, headers: HttpHeaders): Option[URI] =
    headers.location.flatMap { newLocation =>
      Try(URI.create(newLocation)).toOption
    }.map(requestUri.resolve) // redirect could be relative to original URL


  extension (headers: HttpHeaders) def location: Option[String] = headers.firstValue("Location").toScala

  private val httpClient: HttpClient = HttpClient.newBuilder()
    .followRedirects(Redirect.NEVER)
    .connectTimeout(ofSeconds(2))
    .build()

  def requestFor(uri: URI): HttpRequest =
    HttpRequest.newBuilder().uri(uri).timeout(ofSeconds(2)).method("HEAD", noBody()).build()


  def follow(uri: URI): Future[Resp] = httpClient.sendAsync(requestFor(uri), BodyHandlers.discarding()).asScala.map {
    response => redirectGivenBy(uri, response).toRight(response.statusCode == 200)
  }.recover(_ => Left(false))
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
      _.fold(
        ok => Future.successful(Resolved(redirectPath, ok)),
        redirectUri => resolve(redirectPath.adding(redirectUri))
      )
    }

}

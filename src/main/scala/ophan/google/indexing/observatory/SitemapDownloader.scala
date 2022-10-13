package ophan.google.indexing.observatory

import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.Site

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration.ofSeconds
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters._

class SitemapDownloader(implicit
 ec: ExecutionContext
) extends Logging {
  val client: HttpClient = HttpClient.newBuilder.version(HTTP_2).followRedirects(Redirect.NORMAL)
    .connectTimeout(ofSeconds(20)).build

  def fetchSitemapEntriesFor(site: Site): Future[Set[URI]] = Future.traverse(site.sitemaps) { sitemapUrl =>
    client.sendAsync(HttpRequest.newBuilder(sitemapUrl).GET().build(), BodyHandlers.ofInputStream()).asScala.map { response =>
      logger.info(Map(
        "site" -> site.url,
        "sitemap.url" -> sitemapUrl,
        "sitemap.response.statusCode" -> response.statusCode()
      ), s"Received HTTP ${response.statusCode()} response for $sitemapUrl sitemap")
      val uris: Set[URI] = SitemapParser.parse(response.body, site.url)
      logger.info(Map(
        "site" -> site.url,
        "sitemap.url" -> sitemapUrl,
        "sitemap.uris.size" -> uris.size
      ), s"Found ${uris.size} uris in $sitemapUrl sitemap")
      uris
    }
  }.map(_.flatten)
}

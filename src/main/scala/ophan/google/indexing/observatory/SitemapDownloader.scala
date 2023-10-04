package ophan.google.indexing.observatory

import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.Site

import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration.ofSeconds
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*

case class SitemapDownload(site: Site, timestamp: Instant, allUris: Set[URI])

class SitemapDownloader(implicit
 ec: ExecutionContext
) extends Logging {
  val client: HttpClient = HttpClient.newBuilder.version(HTTP_2).followRedirects(Redirect.NORMAL)
    .connectTimeout(ofSeconds(20)).build

  def fetchSitemapEntriesFor(site: Site): Future[SitemapDownload] = {
    val start = Instant.now()
    Future.traverse(site.sitemaps) { sitemapUrl =>
      client.sendAsync(HttpRequest.newBuilder(sitemapUrl).GET().build(), BodyHandlers.ofString()).asScala.map { response =>
        logger.info(Map(
          "site" -> site.url,
          "sitemap.url" -> sitemapUrl,
          "sitemap.response.statusCode" -> response.statusCode()
        ), s"Received HTTP ${response.statusCode()} response for $sitemapUrl sitemap")
        val uris: Set[URI] = SitemapParser.parse(new ByteArrayInputStream(response.body.getBytes()), site.url)
        logger.info(Map(
          "site" -> site.url,
          "sitemap.url" -> sitemapUrl,
          "sitemap.uris.size" -> uris.size
        ), s"Found ${uris.size} uris in $sitemapUrl sitemap")
        uris
      }
    }.map { results =>
      SitemapDownload(site, start, results.flatten)
    }
  }
}

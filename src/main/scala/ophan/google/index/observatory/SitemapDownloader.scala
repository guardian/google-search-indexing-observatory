package ophan.google.index.observatory

import ophan.google.index.observatory.model.Site

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
) {
  val client: HttpClient = HttpClient.newBuilder.version(HTTP_2).followRedirects(Redirect.NORMAL)
    .connectTimeout(ofSeconds(20)).build

  def fetchSitemapEntriesFor(site: Site): Future[Set[URI]] = Future.traverse(site.sitemaps) { sitemapUrl =>
    client.sendAsync(HttpRequest.newBuilder(sitemapUrl).GET().build(), BodyHandlers.ofInputStream()).asScala.map { response =>
      SitemapParser.parse(response.body, site.url)
    }
  }.map(_.flatten)
}

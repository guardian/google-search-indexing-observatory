package ophan.google.indexing.observatory

import com.google.api.client.http.HttpRequest
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.customsearch.v1.CustomSearchAPI
import com.google.api.services.customsearch.v1.model.{Result, Search}
import GoogleSearchService.{reliableSearchTermFor, resultMatches}
import ophan.google.indexing.observatory.model.{CheckReport, ContentSummary, Site}

import java.net.URI
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.CollectionConverters._
import scala.util.Try

class GoogleSearchService(
  apiKey: String
)(implicit
  ec: ExecutionContext
) {
  val search =
    new CustomSearchAPI.Builder(
      new NetHttpTransport,
      new GsonFactory,
      (request: HttpRequest) => {
        request.getHeaders.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")

      }
    ).setApplicationName("search-index-checker").build()

  def contentAvailabilityInGoogleIndex(uri: URI, site: Site): Future[CheckReport] = Future { blocking {
      val listRequest = search.cse.siterestrict.list()
        .setKey(apiKey)
        .setCx(site.searchEngineId) // see https://programmablesearchengine.google.com/controlpanel/all
        .setQ(reliableSearchTermFor(uri))
      CheckReport(Instant.now, accessGoogleIndex = Try(listRequest.execute()).map { googleSearchResponse =>
        findContentMatchInGoogleSearchResponse(googleSearchResponse, uri).isDefined
      })
    }
  }


  def findContentMatchInGoogleSearchResponse(googleSearchResponse: Search, webUrl: URI): Option[com.google.api.services.customsearch.v1.model.Result] = {
    Option(googleSearchResponse.getItems).flatMap { items =>
      items.asScala.toSeq.find { result => resultMatches(webUrl, result) }
    }
  }

}

object GoogleSearchService {
  def resultMatches(webUrl: URI, result: Result): Boolean = Option(result.getLink).exists { link =>
    val resultUri = URI.create(link)
    resultUri.getHost == webUrl.getHost && resultUri.getPath == webUrl.getPath
  }

  /**
   * This string should be something that, when you type it into Google, you
   * reliably should get this content as one of the top hits. The headline of
   * the article would be one candidate for the value, but the headlines can
   * contain characters that are difficult to escape, eg quotes & double-quotes.
   * The path of the webUrl is fairly reliable, so far as I can see.
   */
  def reliableSearchTermFor(uri: URI): String = s""""${uri.getPath}""""
}

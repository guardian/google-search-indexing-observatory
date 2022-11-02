package ophan.google.indexing.observatory

import com.google.api.client.http.HttpRequest
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.customsearch.v1.CustomSearchAPI
import com.google.api.services.customsearch.v1.model.{Result, Search}
import GoogleSearchService.{reliableSearchTermFor, resultMatches}
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.{CheckReport, Site}

import java.net.URI
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.CollectionConverters._
import scala.util.Try

class GoogleSearchService(
  apiKey: String
)(implicit
  ec: ExecutionContext
) extends Logging {
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
    val attemptExecute = Try(listRequest.execute())
    if (attemptExecute.isSuccess)
      logger.debug(s"Successful request for ${site.url}")

    CheckReport(Instant.now, accessGoogleIndex = attemptExecute.map { googleSearchResponse =>
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
   *
   * It seems most reliable to use BOTH the url AND the quoted-path of the url
   * as the search term. When used by themselves, each term fails for some url:
   *
   * * URL-only: Fails to find
   *   - https://www.nytimes.com/video/middle-east
   *   - https://www.nytimes.com/interactive/2021/us/martin-indiana-covid-cases.html
   * * Path-only: Fails to find https://www.theguardian.com/business/live/2022/oct/11/bank-of-england--bond-markets-gilts-uk-unemployment-ifs-spending-cuts-imf-outlook-business-live
   */
  def reliableSearchTermFor(uri: URI): String = s"${uri.toString} \"${uri.getPath}\""
}

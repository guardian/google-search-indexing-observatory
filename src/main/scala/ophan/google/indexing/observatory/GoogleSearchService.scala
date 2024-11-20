package ophan.google.indexing.observatory

import upickle.default.*
import ophan.google.indexing.observatory.logging.Logging
import ophan.google.indexing.observatory.model.{CheckReport, Site}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest}
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.Try

case class DerivedStructData(link: String)

object DerivedStructData {
  implicit val rw: ReadWriter[DerivedStructData] = macroRW
}

case class Document(derivedStructData: DerivedStructData)

object Document {
  implicit val rw: ReadWriter[Document] = macroRW
}

case class SearchResult(document: Document)

object SearchResult {
  implicit val rw: ReadWriter[SearchResult] = macroRW
}

case class SearchResponse(results: List[SearchResult] = List.empty)

object SearchResponse {
  implicit val rw: ReadWriter[SearchResponse] = macroRW
}

class GoogleSearchService(
                           apiKey: String,
                           projectId: String,
                           location: String,
                         )(implicit ec: ExecutionContext) extends Logging {

  private val httpClient = HttpClient.newHttpClient()

  def contentAvailabilityInGoogleIndex(uri: URI, site: Site): Future[CheckReport] = Future {
    blocking {
      val baseUrl = s"https://discoveryengine.googleapis.com/v1/projects/$projectId/locations/$location/dataStores/${site.datastoreId}/servingConfigs/default_config:searchLite"

      val requestBody = ujson.Obj(
        "query" -> GoogleSearchService.reliableSearchTermFor(uri),
        "pageSize" -> 10
      )

      val request = HttpRequest.newBuilder()
        .uri(URI.create(s"$baseUrl?key=$apiKey"))
        .header("Content-Type", "application/json")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
        .build()

      val attemptExecute = Try {
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        val searchResponse = read[SearchResponse](response.body())
        searchResponse.results.exists(result => GoogleSearchService.resultMatches(uri, result))
      }

      if (attemptExecute.isSuccess)
        logger.debug(s"Successful request for ${site.url}")

      CheckReport(Instant.now, accessGoogleIndex = attemptExecute)
    }
  }
}

object GoogleSearchService {
  def resultMatches(webUrl: URI, result: SearchResult): Boolean = {
    val resultUri = URI.create(result.document.derivedStructData.link)
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
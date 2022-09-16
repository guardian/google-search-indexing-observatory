package ophan.google.index.checker

import com.google.api.client.http.{HttpRequest, HttpRequestInitializer}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.customsearch.v1.CustomSearchAPI
import com.google.api.services.customsearch.v1.model.{Result, Search}
import ophan.google.index.checker.GoogleSearchService.resultMatches
import ophan.google.index.checker.model.{ContentAvailabilityInGoogleIndex, ContentSummary}

import java.net.URI
import java.time.Instant
import scala.collection.immutable.SortedMap
import scala.concurrent.{ExecutionContext, Future}
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
      new HttpRequestInitializer {
        override def initialize(request: HttpRequest): Unit = {
          request.getHeaders.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")

        }
      }
    ).setApplicationName("flung").build()


  def contentAvailabilityInGoogleIndex(content: ContentSummary): Future[ContentAvailabilityInGoogleIndex] = Future {
    val listRequest = search.cse.list()
      .setKey(apiKey)
      .setCx("415ef252844d240a7")
      .setQ(content.reliableSearchTerm)
    val checkResult: SortedMap[Instant, Boolean] = Try(listRequest.execute()).map { googleSearchResponse =>
      val matchingGoogleResult = findContentMatchInGoogleSearchResponse(googleSearchResponse, content.webUrl)
      val found = matchingGoogleResult.isDefined
      if (!found) {
        println(content.webUrl)
        println(listRequest.buildHttpRequestUrl().build())
      }
      SortedMap(Instant.now -> found)
    }.getOrElse(SortedMap.empty)


    ContentAvailabilityInGoogleIndex(content, checkResult)
  }


  def findContentMatchInGoogleSearchResponse(googleSearchResponse: Search, webUrl: URI): Option[com.google.api.services.customsearch.v1.model.Result] = {
    Option(googleSearchResponse.getItems).flatMap { items =>
      items.asScala.toSeq.find { result => resultMatches(webUrl, result) }
    }
  }


}

object GoogleSearchService {
  def resultMatches(webUrl: URI, result: Result) = {
    Option(result.getLink).exists { link =>
      val resultUri = URI.create(link)
      resultUri.getHost == webUrl.getHost && resultUri.getPath == webUrl.getPath
    }
  }
}

package ophan.google.index.checker

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

class GoogleSearchService(
  apiKey: String
)(implicit
  ec: ExecutionContext
) {
  val search =
    new CustomSearchAPI.Builder(
      new NetHttpTransport,
      new GsonFactory,
      null
    ).setApplicationName("index-checker").build()


  def contentAvailabilityInGoogleIndex(content: ContentSummary): Future[ContentAvailabilityInGoogleIndex] = Future {
    val googleSearchResponse: Search = search.cse().list()
      .setKey(apiKey)
      .setCx("415ef252844d240a7")
      .setQ(content.reliableSearchTerm)
      .execute()

    val matchingGoogleResult = findContentMatchInGoogleSearchResponse(googleSearchResponse, content.webUrl)
    ContentAvailabilityInGoogleIndex(content, SortedMap(Instant.now -> matchingGoogleResult.isDefined))
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

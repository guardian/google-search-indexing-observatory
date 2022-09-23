package ophan.google.index.checker

import com.gu.contentapi.client.ContentApiClient
import com.gu.contentapi.client.model.SearchQuery
import ophan.google.index.checker.model.ContentSummary

import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS
import scala.concurrent.{ExecutionContext, Future}

class RecentContentService(
  client: com.gu.contentapi.client.ContentApiClient
)(implicit 
  ec: ExecutionContext
) {
  
  val query: SearchQuery = ContentApiClient.search
    .orderBy("newest").useDate("first-publication")
    .showFields("firstPublicationDate")
    .fromDate(Some(Instant.now().minus(4, HOURS)))
    .pageSize(100)
  
  def fetchRecentContent(): Future[Seq[ContentSummary]] = client.getResponse(query) map { resp =>
    val results = resp.results.toSeq
    println(s"Found ${results.size}")
    results.flatMap(ContentSummary.from)
  }

}

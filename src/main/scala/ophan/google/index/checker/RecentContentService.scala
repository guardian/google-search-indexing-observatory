package ophan.google.index.checker

import com.gu.contentapi.client.model.SearchQuery
import com.gu.contentapi.client.{ContentApiClient, GuardianContentClient}
import ophan.google.index.checker.model.ContentSummary

import scala.concurrent.{ExecutionContext, Future}

class RecentContentService(
  client: com.gu.contentapi.client.ContentApiClient
)(implicit 
  ec: ExecutionContext
) {
  
  val query: SearchQuery = ContentApiClient.search
    .orderBy("newest").useDate("first-publication")
    .showFields("firstPublicationDate")
    .pageSize(50)
  
  def fetchRecentContent(): Future[Seq[ContentSummary]] = client.getResponse(query) map {
    _.results.toSeq.flatMap(ContentSummary.from)
  }

}

package ophan.google.indexing.observatory

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class GoogleSearchServiceTest extends AnyFlatSpec with Matchers {
  it should "cope when the google result has tonnes of extra url params" in {
    val googleResultLink = // Actually seen in Google Search UI and Custom Search API results
      "https://www.theguardian.com/food/2022/sep/15/korean-hotdogs-k-dogs-sausage-cheese-fast-food?utm_term=Autofeed&CMP=twt_gu&utm_medium&utm_source=Twitter"

    val canonicalPageUrl = URI.create("https://www.theguardian.com/food/2022/sep/15/korean-hotdogs-k-dogs-sausage-cheese-fast-food")
    GoogleSearchService.resultMatches(canonicalPageUrl, SearchResult(Document(DerivedStructData(googleResultLink)))) shouldBe true
  }

  it should "handle URLs with special characters" in {
    val googleResultLink = "https://www.theguardian.com/food/2023/may/15/café-review"
    val canonicalPageUrl = URI.create("https://www.theguardian.com/food/2023/may/15/café-review")
    GoogleSearchService.resultMatches(canonicalPageUrl, SearchResult(Document(DerivedStructData(googleResultLink)))) shouldBe true
  }

  it should "generate reliable search terms" in {
    val uri = URI.create("https://www.example.com/path/to/article")
    val expectedTerm = "example.com/path/to/article"
    GoogleSearchService.reliableSearchTermFor(uri) shouldBe expectedTerm
  }

  it should "not match different paths on same domain" in {
    val googleResultLink = "https://www.theguardian.com/food/different/path"
    val canonicalPageUrl = URI.create("https://www.theguardian.com/food/original/path")
    GoogleSearchService.resultMatches(canonicalPageUrl, SearchResult(Document(DerivedStructData(googleResultLink)))) shouldBe false
  }
}
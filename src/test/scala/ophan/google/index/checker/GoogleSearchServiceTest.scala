package ophan.google.index.checker

import com.google.api.services.customsearch.v1.model.Result
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class GoogleSearchServiceTest extends AnyFlatSpec with Matchers {
  it should "cope when the google result has tonnes of extra url params" in {
    val googleResultLink = // Actually seen in Google Search UI and Custom Search API results
      "https://www.theguardian.com/food/2022/sep/15/korean-hotdogs-k-dogs-sausage-cheese-fast-food?utm_term=Autofeed&CMP=twt_gu&utm_medium&utm_source=Twitter"

    val canonicalPageUrl = URI.create("https://www.theguardian.com/food/2022/sep/15/korean-hotdogs-k-dogs-sausage-cheese-fast-food")
    GoogleSearchService.resultMatches(canonicalPageUrl, resultWithLink(googleResultLink)) shouldBe true
  }


  private def resultWithLink(googleResultLink: String) = new Result().setLink(googleResultLink)
}

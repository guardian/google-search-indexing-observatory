package ophan.google.indexing.observatory

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SitemapParserTest extends AnyFlatSpec with Matchers {
  it should "Successfully parse our example sitemap" in {
    val stream = getClass.getResource("/independent-sitemap-recent.xml").openStream()

    val output = SitemapParser.parse(stream, "https://www.independent.co.uk")

    output.size shouldBe 200
  }
}

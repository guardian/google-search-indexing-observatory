package ophan.google.indexing.observatory.model

import java.net.URI

case class Site(url: String, searchEngineId: String, sitemaps: Set[URI])

object Sites {
  val NewYorkTimes = Site(
    url="https://www.nytimes.com/",
    searchEngineId="70f791f05cbda4a5c",
    sitemaps=Set(new URI("https://www.nytimes.com/sitemaps/new/news-1.xml.gz"))
  )

  val Independent= Site(
    url = "https://www.independent.co.uk/",
    searchEngineId = "119a788f858c94b0f",
    sitemaps = Set(new URI("https://www.independent.co.uk/sitemaps/googlenews"))
  )

  val All = Set(NewYorkTimes, Independent)
}
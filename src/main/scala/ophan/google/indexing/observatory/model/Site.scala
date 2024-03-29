package ophan.google.indexing.observatory.model

import java.net.URI

case class Site(url: String, searchEngineId: String, sitemaps: Set[URI])

object Sites {
  val NewYorkTimes = Site(
    url="https://www.nytimes.com/",
    searchEngineId="70f791f05cbda4a5c",
    sitemaps=Set(new URI("https://www.nytimes.com/sitemaps/new/news.xml.gz"))
  )

// The Independent appear to change/evolve their URLs often, leaving them in the sitemap with
// redirects to the new URL which is often also in the sitemap, this doesn't work very well for us
// as we don't check if URLs redirect - meaning we think content is missing when its actually just
// been redirected elsewhere.
  val Independent= Site(
    url = "https://www.independent.co.uk/",
    searchEngineId = "119a788f858c94b0f",
    sitemaps = Set(new URI("https://www.independent.co.uk/sitemaps/googlenews"))
  )

  val BBC = Site(
    url = "https://www.bbc.co.uk/",
    searchEngineId = "b44c6a8d0e79b43bb",
    sitemaps = Set(
      new URI("https://www.bbc.co.uk/sitemaps/https-sitemap-uk-news-1.xml"),
      new URI("https://www.bbc.co.uk/sitemaps/https-sitemap-uk-news-2.xml")
    )
  )

  val DailyMail= Site(
    url = "https://www.dailymail.co.uk/",
    searchEngineId = "e42e842597f034061",
    sitemaps = Set(new URI("https://www.dailymail.co.uk/google-news-sitemap1.xml"))
  )

  val WashingtonPost = Site(
    url = "https://www.washingtonpost.com/",
    searchEngineId = "65742b4f9808f4c04",
    sitemaps = Set(new URI("https://www.washingtonpost.com/arcio/news-sitemap/"))
  )

  val Telegraph = Site(
    url = "https://www.telegraph.co.uk/",
    searchEngineId = "94687f1b5a1994152",
    sitemaps = Set(new URI("https://www.telegraph.co.uk/sitemap-news.xml"))
  )

  val Sun = Site( // google-indexing-observatory-
    url = "https://www.thesun.co.uk/",
    searchEngineId = "943ddaf5b540f41a3",
    sitemaps = Set(new URI("https://www.thesun.co.uk/news-sitemap.xml"))
  )

  val All = Set(
    NewYorkTimes,
    BBC,
    DailyMail
  )
}
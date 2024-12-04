package ophan.google.indexing.observatory.model

import java.net.URI

case class Site(
                 url: String,
                 datastoreId: String,
                 sitemaps: Set[URI],
               )

sealed trait SearchStrategy
case object UrlOnly extends SearchStrategy
case object PathOnly extends SearchStrategy
case object UrlAndQuotedPath extends SearchStrategy

object Sites {
  val NewYorkTimes = Site(
    url="https://www.nytimes.com/",
    datastoreId="ophan-nytimes_1732116567495",
    sitemaps=Set(new URI("https://www.nytimes.com/sitemaps/new/news.xml.gz")),
  )

// The Independent appear to change/evolve their URLs often, leaving them in the sitemap with
// redirects to the new URL which is often also in the sitemap, this doesn't work very well for us
// as we don't check if URLs redirect - meaning we think content is missing when its actually just
// been redirected elsewhere.
  val Independent= Site(
    url = "https://www.independent.co.uk/",
    datastoreId = "119a788f858c94b0f",
    sitemaps = Set(new URI("https://www.independent.co.uk/sitemaps/googlenews")),
  )

  val BBC = Site(
    url = "https://www.bbc.co.uk/",
    datastoreId = "ophan-bbc-index_1731497852622",
    sitemaps = Set(
      new URI("https://www.bbc.co.uk/sitemaps/https-sitemap-uk-news-1.xml"),
      new URI("https://www.bbc.co.uk/sitemaps/https-sitemap-uk-news-2.xml")
    ),
  )

  val DailyMail= Site(
    url = "https://www.dailymail.co.uk/",
    datastoreId = "ophan-dailymail_1732116638977",
    sitemaps = Set(new URI("https://www.dailymail.co.uk/google-news-sitemap1.xml")),
  )

  val WashingtonPost = Site(
    url = "https://www.washingtonpost.com/",
    datastoreId = "65742b4f9808f4c04",
    sitemaps = Set(new URI("https://www.washingtonpost.com/arcio/news-sitemap/")),
  )

  val Telegraph = Site(
    url = "https://www.telegraph.co.uk/",
    datastoreId = "94687f1b5a1994152",
    sitemaps = Set(new URI("https://www.telegraph.co.uk/sitemap-news.xml")),
  )

  val Sun = Site(
    url = "https://www.thesun.co.uk/",
    datastoreId = "943ddaf5b540f41a3",
    sitemaps = Set(new URI("https://www.thesun.co.uk/news-sitemap.xml")),
  )

  val All = Set(
    NewYorkTimes,
    BBC,
    DailyMail
  )
}
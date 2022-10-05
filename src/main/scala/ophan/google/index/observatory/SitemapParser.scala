package ophan.google.index.observatory

import com.bnsal.SitemapParser

import java.io.InputStream
import java.net.{URI, URL}

import scala.jdk.CollectionConverters._

object SitemapParser {
  def parse(input: InputStream, url: String): Set[URI] = {
    val sitemapParser = new SitemapParser()
    val sitemap = sitemapParser.parseSitemap(input, new URL(url))
    sitemap.getSitemapEntries().asScala.map(entry => new URI(entry.getLoc)).toSet
  }
}
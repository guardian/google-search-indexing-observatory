package ophan.google.indexing.observatory

import com.gu.http.redirect.resolver.Resolution.Resolved
import com.gu.http.redirect.resolver.UrlResponseFetcher.HttpResponseSummary
import com.gu.http.redirect.resolver.UrlResponseFetcher.HttpResponseSummary.LocationHeader
import com.gu.http.redirect.resolver.{Conclusion, RedirectPath, Resolution, UrlResolver, UrlResponseFetcher}
import ophan.google.indexing.observatory.literals.{uri, *}
import org.scalatest.Inside
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RedirectResolverTest extends AnyFlatSpec with Matchers with ScalaFutures with IntegrationPatience with Inside {
  def resolving[U](uri: URI)(fun: Resolution => U)(using r: UrlResolver) = whenReady(r.resolve(uri))(fun)

  {
    given liveResolver: UrlResolver = UrlResolver(RedirectFollower)

    it should "resolve a NYT url - note the NYT seem to accept curl but not other User-Agents!" in {
      val originalUri = uri"https://www.nytimes.com/2023/10/03/sports/cricket/cricket-world-cup-explained.html"
      resolving(originalUri) {
        inside(_) {
          case resolved: Resolved => resolved.redirectPath.originalUri shouldBe originalUri
        }
      }
    }

    it should "resolve a Daily Mail url" in {
      resolving(uri"https://www.dailymail.co.uk/news/article-12589205/Well-supporting-Ukraine-Biden-tells-allies-President-calls-global-partners-assure-U-S-giving-Zelensky-cash-despite-chaos-Congress-pro-Kremlin-candidate-storming-power-Slovakia.html") {
        inside(_) {
          case resolved: Resolved => resolved.conclusion.isOk shouldBe true
        }
      }
    }

    it should "follow a BBC 'av' page" in {
      resolving(uri"https://www.bbc.co.uk/sport/av/football/66930004") {
         inside(_) {
           case resolved: Resolved => resolved.conclusion.isOk shouldBe true
         }
      }
    }

    it should "follow a BBC url which redirects to an 'av' page" in {
      val expectedRedirects = Seq(
        uri"https://www.bbc.co.uk/news/uk-politics-63534039",
        uri"https://www.bbc.co.uk/news/av/uk-politics-63534039"
      )

      resolving(expectedRedirects.head) {
        _ shouldBe Resolved(RedirectPath(expectedRedirects), Conclusion.Ok)
      }
    }

    it should "not crash for domains that do not exist" in {
      val uriWithNonExistentDomain = uri"https://www.doesnotexist12312312234523532534546.com/"

      resolving(uriWithNonExistentDomain) {
        inside(_) {
          case resolved: Resolved =>
            resolved.redirectPath shouldBe RedirectPath(uriWithNonExistentDomain)
            resolved.conclusion.statusCode.isFailure shouldBe true
        }
      }
    }

    it should "not crash for paths that do not exist" in {
      val uriWithNonExistentPath = uri"https://www.theguardian.com/uk/roberto-is-the-coolest"

      resolving(uriWithNonExistentPath) {
        _ shouldBe Resolved(RedirectPath(Seq(uriWithNonExistentPath)), Conclusion(Success(404)))
      }
    }
  }

  it should "not follow infinite redirects" in {
    val redirectForeverToNewUrls = redirectPathForTesting(_.toIntOption.fold(0)(_ + 1).toString)
    given resolver: UrlResolver = UrlResolver(redirectForeverToNewUrls, maxRedirects = 20)

    resolving(uri"https://example.com/foo") { _.redirectPath.numRedirects shouldBe 20 }
  }

  it should "detect a loop" in {
    val redirectInLoop = redirectPathForTesting(_.toIntOption.fold(0)(pathNum => (pathNum + 1) % 6).toString)
    given UrlResolver = UrlResolver(redirectInLoop, maxRedirects = 20)

    resolving(uri"https://example.com/0") { _.redirectPath.numRedirects shouldBe 6 }
  }

  def redirectPathForTesting(pathChanger: String => String): UrlResponseFetcher = new UrlResponseFetcher {
    override def fetchResponseFor(uri: URI)(implicit ec: ExecutionContext) =
      Future.successful(HttpResponseSummary(301, Some(LocationHeader("/" + pathChanger(uri.getPath.stripPrefix("/"))))))
  }
}

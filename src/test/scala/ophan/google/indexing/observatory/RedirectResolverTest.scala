package ophan.google.indexing.observatory

import ophan.google.indexing.observatory.Resolution.Resolved
import ophan.google.indexing.observatory.literals.*
import org.scalatest.Inside
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import scala.concurrent.Future

class RedirectResolverTest extends AnyFlatSpec with Matchers with ScalaFutures with IntegrationPatience with Inside {
  def resolving[U](uri: URI)(fun: Resolution => U)(using r: RedirectResolver) = whenReady(r.resolve(uri))(fun)

  {
    given liveResolver: RedirectResolver = RedirectResolver(RedirectFollower)

    it should "resolve a NYT url - note the NYT seem to accept curl but not other User-Agents!" in {
      resolving(uri"https://www.nytimes.com/2023/10/03/sports/cricket/cricket-world-cup-explained.html") {
        inside(_) {
          case resolved: Resolved => resolved.ok shouldBe true
        }
      }
    }

    it should "resolve a Daily Mail url" in {
      resolving(uri"https://www.dailymail.co.uk/news/article-12589205/Well-supporting-Ukraine-Biden-tells-allies-President-calls-global-partners-assure-U-S-giving-Zelensky-cash-despite-chaos-Congress-pro-Kremlin-candidate-storming-power-Slovakia.html") {
        inside(_) {
          case resolved: Resolved => resolved.ok shouldBe true
        }
      }
    }

    it should "follow a BBC 'av' page" in {
      resolving(uri"https://www.bbc.co.uk/sport/av/football/66930004") {
         inside(_) {
           case resolved: Resolved => resolved.ok shouldBe true
         }
      }
    }

    it should "follow a BBC url which redirects to an 'av' page" in {
      val expectedRedirects = Seq(
        uri"https://www.bbc.co.uk/news/uk-politics-63534039",
        uri"https://www.bbc.co.uk/news/av/uk-politics-63534039"
      )

      resolving(expectedRedirects.head) {
        _ shouldBe Resolved(RedirectPath(expectedRedirects), ok = true)
      }
    }

    it should "not crash for domains that do not exist" in {
      val uriWithNonExistentDomain = uri"https://www.doesnotexist12312312234523532534546.com/"

      resolving(uriWithNonExistentDomain) {
        _ shouldBe Resolved(RedirectPath(Seq(uriWithNonExistentDomain)), ok = false)
      }
    }

    it should "not crash for paths that do not exist" in {
      val uriWithNonExistentPath = uri"https://www.theguardian.com/uk/roberto-is-the-coolest"

      resolving(uriWithNonExistentPath) {
        _ shouldBe Resolved(RedirectPath(Seq(uriWithNonExistentPath)), ok = false)
      }
    }
  }

  it should "not follow infinite redirects" in {
    val redirectForeverToNewUrls = redirectPathForTesting(_.toIntOption.fold(0)(_ + 1).toString)
    given resolver: RedirectResolver = RedirectResolver(redirectForeverToNewUrls, maxRedirects = 20)

    resolving(uri"https://example.com/foo") { _.redirectPath.numRedirects shouldBe resolver.maxRedirects }
  }

  it should "detect a loop" in {
    val redirectInLoop = redirectPathForTesting(_.toIntOption.fold(0)(pathNum => (pathNum + 1) % 6).toString)
    given RedirectResolver = RedirectResolver(redirectInLoop, maxRedirects = 20)

    resolving(uri"https://example.com/0") { _.redirectPath.numRedirects shouldBe 6 }
  }

  def redirectPathForTesting(pathChanger: String => String): RedirectFollower = (uri: URI) =>
    Future.successful(Right(new URI(uri.getScheme, uri.getHost, "/" + pathChanger(uri.getPath.stripPrefix("/")), uri.getFragment)))
}

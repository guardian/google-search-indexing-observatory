package ophan.google.index.checker.model

import java.time.Instant
import scala.util.Try

case class CheckReport(time: Instant, accessGoogleIndex: Try[Boolean])

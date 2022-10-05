package ophan.google.index.observatory.model

import java.time.Instant
import scala.util.Try

case class CheckReport(time: Instant, accessGoogleIndex: Try[Boolean])

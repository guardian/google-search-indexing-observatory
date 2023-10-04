package ophan.google.indexing.observatory.logging

import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.{Logger, LoggerFactory}

import java.net.URI
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

trait Logging {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit def mapToContext(c: Map[String, _]): LogstashMarker = appendEntries(c.asJava)

  def contextSampleOf(prefix: String, coll: Iterable[URI]): Map[String, _] = Map(
    s"$prefix.count" -> coll.size,
    s"$prefix.sample" -> coll.take(3).asJava
  )
  
}

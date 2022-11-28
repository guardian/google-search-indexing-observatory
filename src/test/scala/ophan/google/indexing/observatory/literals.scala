package ophan.google.indexing.observatory

import org.typelevel.literally.Literally

import java.net.URI
import scala.util.Try

object literals:
  extension (inline ctx: StringContext)
    inline def uri(inline args: Any*): URI = ${URILiteral('ctx, 'args)}

  object URILiteral extends Literally[URI]:
    def validate(s: String)(using Quotes) =
      Try(URI.create(s)).toEither.left.map(_.getMessage).map(_ => '{URI.create(${Expr(s)})})

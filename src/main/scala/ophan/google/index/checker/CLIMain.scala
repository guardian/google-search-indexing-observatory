package ophan.google.index.checker

/**
 * This class isn't used in production, it's only for local testing.
 *
 * It's separate from the Lambda class because adding the scala.App
 * trait pull in scala.DelayedInit, which seems to lead to
 * java.lang.NullPointerException errors when invoked as a Lambda -
 * vals are not initialised by the time we come to service the request?!
 */
object CLIMain extends App {

  Lambda.go()
}

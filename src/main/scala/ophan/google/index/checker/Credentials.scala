package ophan.google.index.checker

import ophan.google.index.checker.logging.Logging

case object Credentials extends Logging {
  def fetchKeyFromParameterStore(value: String): String = {
    val paramName = s"/PROD/ophan/google-search-index-checker/$value"
    logger.info(Map(
      "credentials.paramName" -> paramName,
    ), s"Loading param: '$paramName'")
    val v = AWS.SSM.getParameter(_.withDecryption(true).name(paramName)).parameter.value
    logger.info(Map(
      "credentials.paramName" -> paramName,
    ), s"Successfully loaded param: '$paramName'")
    v
  }
}

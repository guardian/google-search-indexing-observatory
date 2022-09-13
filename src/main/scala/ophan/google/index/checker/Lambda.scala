package ophan.google.index.checker

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import ophan.google.index.checker.logging.Logging
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import scala.util.Failure
import scala.util.Success

object Lambda extends Logging {

  /*
   * Logic handler
   */
  def go(): Unit = {

  }

  // See https://docs.aws.amazon.com/AmazonS3/latest/userguide/finding-canonical-user-id.html
  val DeployToolsAWSAccountCanonicalUserId = "4545b54bd17af766e5e14aa12fd41bade300cf170dc6f5c4cd09240d36484cf1"


  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: ScheduledEvent, context: Context): Unit = {
    go()
  }

}

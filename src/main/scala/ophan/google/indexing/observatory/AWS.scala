package ophan.google.indexing.observatory

import software.amazon.awssdk.auth.credentials._
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1
import software.amazon.awssdk.services.dynamodb.{DynamoDbAsyncClient, DynamoDbAsyncClientBuilder}
import software.amazon.awssdk.services.ssm.{SsmClient, SsmClientBuilder}

object AWS {
  val region: Region = EU_WEST_1

  def credentialsForDevAndProd(devProfile: String, prodCreds: AwsCredentialsProvider): AwsCredentialsProviderChain =
    AwsCredentialsProviderChain.of(prodCreds, ProfileCredentialsProvider.builder().profileName(devProfile).build())

  lazy val credentials: AwsCredentialsProvider =
    credentialsForDevAndProd("ophan", EnvironmentVariableCredentialsProvider.create())

  def build[T, B <: AwsClientBuilder[B, T]](builder: B): T =
    builder.credentialsProvider(credentials).region(region).build()

  private val sdkHttpClient: SdkHttpClient = UrlConnectionHttpClient.builder().build()
  val SSM = build[SsmClient, SsmClientBuilder](SsmClient.builder().httpClient(sdkHttpClient))
  val dynamoDb = build[DynamoDbAsyncClient, DynamoDbAsyncClientBuilder](DynamoDbAsyncClient.builder())
}

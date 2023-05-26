import com.gu.riffraff.artifact.BuildInfo

name := "google-search-indexing-observatory"

organization := "com.gu"

description:= "Checking how long it takes content published by news organisations to be available in Google search"

version := "1.0"

scalaVersion := "3.2.2"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8"
)

val catsVersion = "2.9.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.1",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.3",
  "org.slf4j" % "log4j-over-slf4j" % "2.0.7", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.lihaoyi" %% "upickle" % "3.1.0",

  "com.madgag" %% "scala-collection-plus" % "0.11",
  "com.google.http-client" % "google-http-client-gson" % "1.43.2",
  "com.google.apis" % "google-api-services-customsearch" % "v1-rev20230319-2.0.0",
  "org.scanamo" %% "scanamo" % "1.0.0-M26",
  "org.scalatest" %% "scalatest" % "3.2.16" % Test,
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "alleycats-core" % catsVersion,

  "com.bnsal" % "sitemap-parser" % "1.0.3"

) ++ Seq("ssm", "url-connection-client").map(artifact => "software.amazon.awssdk" % artifact % "2.19.33")

Test / testOptions +=
  Tests.Argument(TestFrameworks.ScalaTest, "-u", s"test-results/scala-${scalaVersion.value}")

enablePlugins(RiffRaffArtifact, BuildInfoPlugin)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffArtifactResources := Seq(
  (assembly/assemblyOutputPath).value -> s"${name.value}/${name.value}.jar",
  file("cdk/cdk.out/GoogleSearchIndexingObservatory-PROD.template.json") -> s"cdk.out/GoogleSearchIndexingObservatory-PROD.template.json",
  file("cdk/cdk.out/riff-raff.yaml") -> s"riff-raff.yaml"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}

buildInfoPackage := "ophan.google.indexing.observatory"
buildInfoKeys := {
  lazy val buildInfo = BuildInfo(baseDirectory.value)
  Seq[BuildInfoKey](
    "buildNumber" -> buildInfo.buildIdentifier,
    "gitCommitId" -> buildInfo.revision,
    "buildTime" -> System.currentTimeMillis
  )
}

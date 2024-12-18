import com.gu.riffraff.artifact.BuildInfo

name := "google-search-indexing-observatory"

organization := "com.gu"

description:= "Checking how long it takes content published by news organisations to be available in Google search"

version := "1.0"

scalaVersion := "3.3.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8"
)

val catsVersion = "2.10.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.4",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
  "org.slf4j" % "log4j-over-slf4j" % "2.0.12", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime
  "ch.qos.logback" % "logback-classic" % "1.5.0",
  "com.lihaoyi" %% "upickle" % "3.2.0",
  "com.squareup.okhttp3" % "okhttp" % "4.12.0",

  "com.madgag" %% "scala-collection-plus" % "0.11",
  "org.scanamo" %% "scanamo" % "1.0.0-M30",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "alleycats-core" % catsVersion,

  "com.bnsal" % "sitemap-parser" % "1.0.3",
  "org.typelevel" %% "literally" % "1.1.0" % Test,

  "com.github.blemale" %% "scaffeine" % "5.2.1",
  "com.gu" %% "redirect-resolver" % "0.0.35"

) ++ Seq("ssm", "url-connection-client").map(artifact => "software.amazon.awssdk" % artifact % "2.25.28")

Test / testOptions +=
  Tests.Argument(TestFrameworks.ScalaTest, "-u", s"test-results/scala-${scalaVersion.value}", "-o")

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

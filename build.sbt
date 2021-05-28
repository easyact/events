import sbtassembly.Log4j2MergeStrategy
import sbtrelease.Version

name := "events"

resolvers += Resolver.sonatypeRepo("public")
scalaVersion := "2.12.10"
//scalaVersion := "2.13.5"
releaseNextVersion := { ver =>
  Version(ver).map(_.bumpMinor.string).getOrElse("Error")
}
assemblyJarName in assembly := "events.jar"

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/org.scalaz/scalaz-concurrent
  "org.scalaz" %% "scalaz-concurrent" % "7.3.0-M27",
  "com.amazonaws" % "aws-lambda-java-events" % "3.8.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  //  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.1026",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.3",
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

assemblyMergeStrategy in assembly := {
  case "module-info.class" => MergeStrategy.discard
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" =>
    Log4j2MergeStrategy.plugincache
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

import sbtassembly.Log4j2MergeStrategy
import sbtrelease.Version

name := "events"

resolvers += Resolver.sonatypeRepo("public")
scalaVersion := "2.13.5"
releaseNextVersion := { ver =>
  Version(ver).map(_.bumpMinor.string).getOrElse("Error")
}
assemblyJarName in assembly := "events.jar"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "3.8.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.1026",
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

assemblyMergeStrategy in assembly := {
  case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" =>
    Log4j2MergeStrategy.plugincache
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

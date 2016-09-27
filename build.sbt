name := "socialsearch"

version := "2.0.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Ywarn-unused-import")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.10",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.10",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.10.0",
  "com.typesafe.play" %% "play-json" % "2.5.6",
  "com.typesafe.play" %% "play-ws" % "2.5.6",
  "com.github.gphat" %% "wabisabi" % "2.1.7")

mainClass in Compile := Some("SocialSearch")

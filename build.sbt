name := "socialsearch-indexer"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Ywarn-unused-import")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.2",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.2",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.9.0",
  "ch.qos.logback" % "logback-classic" % "1.1.6",
  "com.typesafe.play" %% "play-json" % "2.5.5",
  "com.github.gphat" %% "wabisabi" % "2.1.7",
  "com.typesafe.play" %% "play-ws" % "2.5.6",
  "com.typesafe" % "config" % "1.3.0")

name := "socialsearch-indexer"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.5.5",
  "com.github.gphat" %% "wabisabi" % "2.1.7",
  "com.typesafe.play" %% "play-ws" % "2.5.6")

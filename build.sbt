name := "socialsearch-indexer"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.5"

//libraryDependencies += "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.3.1"

libraryDependencies += "com.github.gphat" %% "wabisabi" % "2.1.7"

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.5.6"

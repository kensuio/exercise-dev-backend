name := "forex"
version := "1.0.0"

scalaVersion := "2.12.12"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-Ypartial-unification",
  "-language:experimental.macros",
  "-language:implicitConversions"
)

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "com.github.pureconfig"      %% "pureconfig"           % "0.13.0",
  "com.softwaremill.quicklens" %% "quicklens"            % "1.6.1",
  "com.typesafe.akka"          %% "akka-stream"          % "2.6.6",
  "com.typesafe.akka"          %% "akka-http"            % "10.1.12",
  "de.heikoseeberger"          %% "akka-http-circe"      % "1.33.0",
  "io.circe"                   %% "circe-core"           % "0.13.0",
  "io.circe"                   %% "circe-generic"        % "0.13.0",
  "io.circe"                   %% "circe-generic-extras" % "0.13.0",
//  "io.circe"                   %% "circe-java8"          % "0.13.0",
  "io.circe"  %% "circe-jawn" % "0.13.0",
  "org.atnos" %% "eff"        % "5.10.0",
  "org.atnos" %% "eff-monix"  % "5.10.0",
//  "org.typelevel"              %% "cats-core"            % "0.9.0",
  "org.zalando"                %% "grafter"        % "2.6.1",
  "ch.qos.logback"             % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"
//  compilerPlugin("org.typelevel"   %% "kind-projector" % "0.11.0" cross CrossVersion.full),
//  compilerPlugin("org.scalamacros" %% "paradise"       % "2.1.1" cross CrossVersion.full)
)

addCompilerPlugin("org.typelevel"   %% "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("org.scalamacros" %% "paradise"       % "2.1.1" cross CrossVersion.full)

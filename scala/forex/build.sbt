name := "forex"
version := "1.0.0"

scalaVersion := "2.13.6"
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ymacro-annotations",
  "-Wdead-code",
  "-Werror",
  "-Wnumeric-widen",
  "-Xlint:-unused"
)

// Needed for tests written with zio-tests
Test / testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

val zioVersion = "1.0.9"
val zioConfigVersion = "1.0.6"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio"                 % zioVersion,
  "dev.zio" %% "zio-macros"          % zioVersion,
  "dev.zio" %% "zio-streams"         % zioVersion,
  "dev.zio" %% "zio-logging-slf4j"   % "0.5.11",
  "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
  "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
  "dev.zio" %% "zio-test-sbt"        % zioVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"          % "2.6.14",
  "com.typesafe.akka" %% "akka-http"            % "10.2.4",
  "de.heikoseeberger" %% "akka-http-circe"      % "1.37.0",
  "io.circe"          %% "circe-core"           % "0.14.1",
  "io.circe"          %% "circe-generic"        % "0.14.1",
  "io.circe"          %% "circe-generic-extras" % "0.14.1",
  "io.circe"          %% "circe-jawn"           % "0.14.1",
  "ch.qos.logback"    % "logback-classic"       % "1.2.3"
)

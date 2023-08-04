name := "forex"
version := "1.0.0"

scalaVersion := "2.13.10"
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

// Uncomment for tests written using zio-tests
//Test / testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

val zioVersion       = "2.0.15"
val zioConfigVersion = "3.0.7"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio"                 % zioVersion,
  "dev.zio" %% "zio-macros"          % zioVersion,
  "dev.zio" %% "zio-streams"         % zioVersion,
  "dev.zio" %% "zio-logging-slf4j"   % "2.1.12",
  "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
  "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
  "dev.zio" %% "zio-cache"           % "0.2.3",
  "dev.zio" %% "zio-http"            % "3.0.0-RC2",
  "dev.zio" %% "zio-json"            % "0.5.0",
  "dev.zio" %% "zio-test-sbt"        % zioVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"        % "2.6.21",
  "com.typesafe.akka" %% "akka-http"          % "10.2.10",
  "de.heikoseeberger" %% "akka-http-zio-json" % "1.39.2",
  "ch.qos.logback"     % "logback-classic"    % "1.4.7"
)

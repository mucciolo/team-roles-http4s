lazy val Http4sVersion = "0.23.18"
lazy val CirceVersion = "0.14.5"
lazy val MunitVersion = "0.7.29"
lazy val LogbackVersion = "1.4.7"
lazy val MunitCatsEffectVersion = "1.0.7"
lazy val DoobieVersion = "1.0.0-RC1"
lazy val FlywayVersion = "9.16.0"
lazy val PureConfigVersion = "0.17.4"
lazy val ScalaTestVersion = "3.2.15"
lazy val ScalaMockVersion = "5.2.0"
lazy val WireMockVersion = "2.35.0"
lazy val CatsEffectTestingVersion = "1.5.0"
lazy val ApacheCommonsVersion = "3.12.0"
lazy val TestContainersVersion = "0.40.12"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    organization := "com.mucciolo",
    name := "team-roles-http4s",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-ember-server"             % Http4sVersion,
      "org.http4s"             %% "http4s-ember-client"             % Http4sVersion,
      "org.http4s"             %% "http4s-circe"                    % Http4sVersion,
      "org.http4s"             %% "http4s-dsl"                      % Http4sVersion,

      "io.circe"               %% "circe-generic"                   % CirceVersion,
      "io.circe"               %% "circe-parser"                    % CirceVersion,

      "com.github.pureconfig"  %% "pureconfig"                      % PureConfigVersion,
      "com.github.pureconfig"  %% "pureconfig-cats-effect"          % PureConfigVersion,
      "com.github.pureconfig"  %% "pureconfig-http4s"               % PureConfigVersion,

      "org.tpolecat"           %% "doobie-core"                     % DoobieVersion,
      "org.tpolecat"           %% "doobie-postgres"                 % DoobieVersion,
      "org.tpolecat"           %% "doobie-hikari"                   % DoobieVersion,

      "org.flywaydb"           % "flyway-core"                      % FlywayVersion,

      "org.apache.commons"     % "commons-lang3"                    % ApacheCommonsVersion,

      "ch.qos.logback"         % "logback-classic"                  % LogbackVersion,

      "org.scalatest"          %% "scalatest"                       % ScalaTestVersion         % "test, it",
      "org.scalamock"          %% "scalamock"                       % ScalaMockVersion         % "test",
      "com.github.tomakehurst" % "wiremock-jre8"                    % WireMockVersion          % "it",
      "org.typelevel"          %% "cats-effect-testing-scalatest"   % CatsEffectTestingVersion % "test, it",
      "com.dimafeng"           %% "testcontainers-scala-scalatest"  % TestContainersVersion    % "it",
      "com.dimafeng"           %% "testcontainers-scala-postgresql" % TestContainersVersion    % "it"

    ),
    Defaults.itSettings,
    Test / testForkedParallel := true,
    Test / fork := true,
    IntegrationTest / testForkedParallel := true,
    IntegrationTest / fork := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "utf-8",
      "-release", "11",
      "-explaintypes",
      "-feature",
      "-unchecked",

      "-Xcheckinit",
      "-Xlint:adapted-args",
      "-Xlint:constant",
      "-Xlint:delayedinit-select",
      "-Xlint:doc-detached",
      "-Xlint:implicit-recursion",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Xlint:nonlocal-return",
      "-Xlint:implicit-not-found",
      "-Xlint:valpattern",
      "-Xlint:eta-zero",
      "-Xlint:eta-sam",
      "-Xlint:deprecation",

      "-Wdead-code",
      "-Wextra-implicit",
      "-Wmacros:both",
      "-Wnumeric-widen",
      "-Woctal-literal",
      "-Wunused:imports",
      "-Wunused:patvars",
      "-Wunused:privates",
      "-Wunused:locals",
      "-Wunused:explicits",
      "-Wunused:implicits",
      "-Wunused:params",
      "-Wunused:linted",
      "-Wvalue-discard",

      "-Ybackend-parallelism", "8",
      "-Ymacro-annotations"
    )
  )

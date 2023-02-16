lazy val Http4sVersion = "0.23.18"
lazy val CirceVersion = "0.14.4"
lazy val MunitVersion = "0.7.29"
lazy val LogbackVersion = "1.3.5"
lazy val MunitCatsEffectVersion = "1.0.7"
lazy val DoobieVersion = "1.0.0-RC1"
lazy val FlywayVersion = "9.14.1"
lazy val PureConfigVersion = "0.17.2"
lazy val ScalaTestVersion = "3.2.15"
lazy val ScalaMockVersion = "5.2.0"
lazy val WireMockVersion = "2.35.0"
lazy val CatsEffectTestingVersion = "1.5.0"
lazy val ApacheCommonsVersion = "3.12.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.mucciolo",
    name := "team-roles",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-ember-server"           % Http4sVersion,
      "org.http4s"             %% "http4s-ember-client"           % Http4sVersion,
      "org.http4s"             %% "http4s-circe"                  % Http4sVersion,
      "org.http4s"             %% "http4s-dsl"                    % Http4sVersion,

      "io.circe"               %% "circe-generic"                 % CirceVersion,
      "io.circe"               %% "circe-parser"                  % CirceVersion,

      "ch.qos.logback"         % "logback-classic"                % LogbackVersion,

      "com.github.pureconfig"  %% "pureconfig"                    % PureConfigVersion,
      "com.github.pureconfig"  %% "pureconfig-cats-effect"        % PureConfigVersion,
      "com.github.pureconfig"  %% "pureconfig-http4s"             % PureConfigVersion,

      "org.tpolecat"           %% "doobie-core"                   % DoobieVersion,
      "org.tpolecat"           %% "doobie-postgres"               % DoobieVersion,
      "org.tpolecat"           %% "doobie-hikari"                 % DoobieVersion,

      "org.flywaydb"           % "flyway-core"                    % FlywayVersion,

      "org.apache.commons"     % "commons-lang3"                  % ApacheCommonsVersion,

      "org.scalatest"          %% "scalatest"                     % ScalaTestVersion         % Test,
      "org.scalamock"          %% "scalamock"                     % ScalaMockVersion         % Test,
      "com.github.tomakehurst" % "wiremock-jre8"                  % WireMockVersion          % Test,
      "org.typelevel"          %% "cats-effect-testing-scalatest" % CatsEffectTestingVersion % Test
    ),
    Defaults.itSettings,
    fork := true,
    scalacOptions += "-Ymacro-annotations"
  )

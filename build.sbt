lazy val Http4sVersion = "0.23.18"
lazy val CirceVersion = "0.14.4"
lazy val MunitVersion = "0.7.29"
lazy val LogbackVersion = "1.4.5"
lazy val MunitCatsEffectVersion = "1.0.7"
lazy val DoobieVersion = "1.0.0-RC1"
lazy val FlywayVersion = "9.14.1"
lazy val PureConfigVersion = "0.17.2"
lazy val ScalaTestVersion = "3.2.15"
lazy val ScalaMockVersion = "5.2.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.mucciolo",
    name := "team-roles",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-ember-server"    % Http4sVersion,
      "org.http4s"            %% "http4s-ember-client"    % Http4sVersion,
      "org.http4s"            %% "http4s-circe"           % Http4sVersion,
      "org.http4s"            %% "http4s-dsl"             % Http4sVersion,

      "io.circe"              %% "circe-generic"          % CirceVersion,

      "ch.qos.logback"        %  "logback-classic"        % LogbackVersion,

      "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,

      "org.tpolecat"          %% "doobie-core"            % DoobieVersion,
      "org.tpolecat"          %% "doobie-postgres"        % DoobieVersion,
      "org.tpolecat"          %% "doobie-hikari"          % DoobieVersion,

      "org.flywaydb"          %  "flyway-core"            % FlywayVersion,

      "org.scalatest"         %% "scalatest"              % ScalaTestVersion  % "test, it",
      "org.scalamock"         %% "scalamock"              % ScalaMockVersion  % "test"
    ),
    scalacOptions += "-Ymacro-annotations"
  )

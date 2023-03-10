package com.mucciolo.teamroles.server

import cats.effect.{IO, Resource}
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import com.mucciolo.teamroles.config.{AppConf, ServerConf}
import com.mucciolo.teamroles.core.RoleServiceImpl
import com.mucciolo.teamroles.repository.PostgresRoleRepository
import com.mucciolo.teamroles.routes.RoleRoutes
import com.mucciolo.teamroles.userteams.HttpUserTeamsClient
import com.mucciolo.teamroles.util.Database
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.{HttpApp, HttpRoutes}
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import pureconfig.module.http4s._

object HttpServer {

  private object Default {
    val host = ipv4"127.0.0.1"
    val port = port"8080"
  }

  def runForever(): IO[Nothing] = {
    for {
      config <- Resource.eval(ConfigSource.default.loadF[IO, AppConf]())
      transactor <- Database.newTransactorResource(config.database)
      _ <- Database.newMigrationResource(transactor)
      roleRepository = new PostgresRoleRepository(transactor)
      httpClient <- EmberClientBuilder.default[IO].build
      userTeamsClient = new HttpUserTeamsClient(httpClient, config.userTeamsClient)
      roleService = new RoleServiceImpl(roleRepository, userTeamsClient)
      roleRoutes = RoleRoutes(roleService)
      _ <- buildEmberServer(config.server, roleRoutes)
    } yield ()
  }.useForever

  private def buildEmberServer(config: ServerConf, routes: HttpRoutes[IO]): Resource[IO, Server] = {

    val httpApp: HttpApp[IO] = CORS.policy.apply(
      Logger.httpApp(logHeaders = config.logHeaders, logBody = config.logBody)(routes.orNotFound)
    )

    EmberServerBuilder.default[IO]
      .withHost(Host.fromString(config.host).getOrElse(Default.host))
      .withPort(Port.fromInt(config.port).getOrElse(Default.port))
      .withHttpApp(httpApp)
      .build
  }
}

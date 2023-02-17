package com.mucciolo.server

import cats.effect.{IO, Resource}
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import com.mucciolo.client.userteams.HttpUserTeamsClient
import com.mucciolo.config.{AppConf, ServerConf}
import com.mucciolo.database.Database
import com.mucciolo.repository.SQLRoleRepository
import com.mucciolo.routes.RoleRoutes
import com.mucciolo.service.RoleServiceImpl
import org.http4s.HttpRoutes
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import pureconfig.module.http4s._

import scala.concurrent.ExecutionContext

object HttpServer {

  private object Default {
    val host = ipv4"127.0.0.1"
    val port = port"8080"
  }

  def run(): IO[Nothing] = {
    for {
      config <- Resource.eval(ConfigSource.default.loadF[IO, AppConf]())
      transactor <- Database.transactor(config.database, ExecutionContext.global)
      _ <- Database.migrate(transactor)
      roleRepository = new SQLRoleRepository(transactor)
      httpClient <- EmberClientBuilder.default[IO].build
      userTeamsClient = new HttpUserTeamsClient(httpClient, config.userTeamsClient)
      roleService = new RoleServiceImpl(roleRepository, userTeamsClient)
      roleRoutes = RoleRoutes(roleService)
      _ <- buildEmberServer(config.server, roleRoutes)
    } yield ()
  }.useForever

  private def buildEmberServer(config: ServerConf, routes: HttpRoutes[IO]): Resource[IO, Server] = {

    val httpApp = Logger.httpApp(
      logHeaders = config.logHeaders, logBody = config.logBody
    )(routes.orNotFound)

    EmberServerBuilder.default[IO]
      .withHost(Host.fromString(config.host).getOrElse(Default.host))
      .withPort(Port.fromInt(config.port).getOrElse(Default.port))
      .withHttpApp(httpApp)
      .build
  }
}

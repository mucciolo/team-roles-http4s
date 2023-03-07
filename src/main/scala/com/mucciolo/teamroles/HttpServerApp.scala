package com.mucciolo.teamroles

import cats.effect.{IO, IOApp}
import com.mucciolo.teamroles.server.HttpServer

object HttpServerApp extends IOApp.Simple {
  def run: IO[Unit] = HttpServer.runForever()
}

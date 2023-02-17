package com.mucciolo.database

import cats.effect._
import com.mucciolo.config.DatabaseConf
import doobie.hikari.HikariTransactor
import doobie.hikari.HikariTransactor.newHikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object Database {

  def transactor(
    config: DatabaseConf,
    executionContext: ExecutionContext)
  : Resource[IO, HikariTransactor[IO]] = {
    newHikariTransactor[IO](config.driver, config.url, config.user, config.pass,executionContext)
  }

  def migrate(transactor: HikariTransactor[IO]): Resource[IO, Unit] = {
    Resource.eval {
      transactor.configure { dataSource =>
        IO {
          Flyway.configure().dataSource(dataSource).load().migrate()
          ()
        }
      }
    }
  }

}

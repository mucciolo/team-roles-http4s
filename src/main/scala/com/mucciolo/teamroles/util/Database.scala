package com.mucciolo.teamroles.util

import cats.effect._
import com.mucciolo.teamroles.config.DatabaseConf
import doobie.hikari.HikariTransactor.newHikariTransactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway

import javax.sql.DataSource
import scala.concurrent.ExecutionContext

object Database {

  def newTransactor(
    config: DatabaseConf,
    executionContext: ExecutionContext
  ): Resource[IO, Transactor.Aux[IO, _ <: DataSource]] = {
    newHikariTransactor[IO](config.driver, config.url, config.user, config.pass, executionContext)
  }

  def migrate(transactor: Transactor.Aux[IO, _ <: DataSource]): Resource[IO, Unit] = {
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

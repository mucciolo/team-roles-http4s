package com.mucciolo.teamroles.util

import cats.effect._
import com.mucciolo.teamroles.config.DatabaseConf
import doobie.hikari.HikariTransactor.newHikariTransactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway

import java.util.concurrent.Executors
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

object Database {

  def newTransactor(
    config: DatabaseConf
  ): Resource[IO, Transactor.Aux[IO, _ <: DataSource]] = {
    val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
    newHikariTransactor[IO](config.driver, config.jdbcUrl, config.user, config.password, ec)
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

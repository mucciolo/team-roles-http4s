package com.mucciolo.teamroles.util

import cats.effect._
import com.mucciolo.teamroles.config.DatabaseConf
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import doobie.hikari.HikariTransactor.newHikariTransactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import java.util.concurrent.Executors
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

object Database {

  type DataSourceTransactor = Transactor.Aux[IO, _ <: DataSource]

  def newTransactorResource(config: DatabaseConf): Resource[IO, DataSourceTransactor] = {
    val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
    newHikariTransactor[IO](config.driver, config.jdbcUrl, config.user, config.password, ec)
  }

  def newMigrationResource(transactor: DataSourceTransactor): Resource[IO, MigrateResult] = {
    Resource.eval(transactor.configure(dataSource => IO(migrate(dataSource))))
  }

  def migrate(dataSource: DataSource): MigrateResult = {
    Flyway.configure().dataSource(dataSource).load().migrate()
  }

  def newTransactor(dataSource: HikariDataSource): DataSourceTransactor = {
    val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
    HikariTransactor[IO](dataSource, ec)
  }
}

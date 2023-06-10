package com.mucciolo.teamroles.repository

import cats.effect.testing.scalatest.{AssertingSyntax, AsyncIOSpec}
import cats.implicits._
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.mucciolo.teamroles.util.Database
import com.mucciolo.teamroles.util.Database.DataSourceTransactor
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.postgres.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues, OptionValues}
import org.testcontainers.utility.DockerImageName

trait PostgresIntegrationSpec extends AsyncWordSpec with Matchers with EitherValues with AsyncIOSpec
  with OptionValues with AssertingSyntax with ForAllTestContainer with BeforeAndAfterEach {

  override val container: PostgreSQLContainer = PostgreSQLContainer(
    dockerImageNameOverride = DockerImageName.parse("postgres:15.2-alpine")
  )

  private lazy val dataSource: HikariDataSource = {
    val conf = new HikariConfig()
    conf.setJdbcUrl(container.jdbcUrl)
    conf.setUsername(container.username)
    conf.setPassword(container.password)

    new HikariDataSource(conf)
  }

  protected lazy val transactor: DataSourceTransactor = Database.newTransactor(dataSource)

  override def afterStart(): Unit = Database.migrate(dataSource)

  protected def clearDatabase(transactor: DataSourceTransactor): Unit

  override protected def beforeEach(): Unit = clearDatabase(transactor)

}

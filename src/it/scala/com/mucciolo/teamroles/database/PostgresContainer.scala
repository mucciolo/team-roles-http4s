package com.mucciolo.teamroles.database

import cats.implicits._
import com.dimafeng.testcontainers.DockerComposeContainer.ComposeFile
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import com.mucciolo.teamroles.config.DatabaseConf
import org.testcontainers.containers.wait.strategy.Wait

import java.io.File

object PostgresContainer {

  val Def: DockerComposeContainer.Def =
    DockerComposeContainer.Def(
      ComposeFile(Left(new File("src/it/resources/docker-compose.yml"))),
      tailChildContainers = true,
      exposedServices = Seq(
        ExposedService(
          "postgres",
          5433,
          Wait.forLogMessage(".*database system is ready to accept connections.*", 2)
        )
      )
    )

  val Conf: DatabaseConf = DatabaseConf(
    driver = "org.postgresql.Driver",
    jdbcUrl = "jdbc:postgresql://localhost:5433/test",
    user = "test",
    password = "test"
  )

}

package com.mucciolo.repository

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.util.UUID
import scala.language.postfixOps

final class SQLRoleRepository(transactor: Transactor[IO]) extends RoleRepository {

  override def insert(role: RoleInsert): EitherT[IO, String, Role] = {
    EitherT(
      sql"INSERT INTO roles (name) VALUES (${role.name})".update
        .withUniqueGeneratedKeys[Role]("id", "name")
        .transact(transactor)
        .attemptSql
    )
      .leftMap { error =>
        if (error.getMessage.contains(s"Key (name)=(${role.name}) already exists"))
          "Role name already exists"
        else
          "Unexpected error"
      }
  }

  def findById(id: UUID): IO[Option[Role]] = {
    sql"SELECT * FROM data WHERE id = ${id.toString}".query[Role].option.transact(transactor)
  }

}

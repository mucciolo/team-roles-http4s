package com.mucciolo.teamroles.repository

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.implicits._
import com.mucciolo.teamroles.core.Domain._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.util.UUID
import scala.language.postfixOps

final class SQLRoleRepository(transactor: Transactor[IO]) extends RoleRepository {

  private val UnmappedError = Error("*", "unmapped")

  override def insert(roleName: String): EitherT[IO, Error, Role] = {
    EitherT(
      sql"""
            INSERT INTO roles (name)
            VALUES ($roleName)
         """
        .update
        .withUniqueGeneratedKeys[Role]("id", "name")
        .transact(transactor)
        .attemptSql
    )
      .leftMap { error =>
        if (error.getMessage.contains(s"Key (name)=($roleName) already exists"))
          Error("role.name", "already.exists")
        else
          UnmappedError
      }
  }

  override def upsertMembershipRole(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, Error, Boolean] = {
    EitherT(
      sql"""
            INSERT INTO team_member_role
            VALUES ($teamId, $userId, $roleId)
            ON CONFLICT (team_id, user_id) DO UPDATE SET role_id = excluded.role_id
         """
        .update
        .run
        .transact(transactor)
        .attemptSql
    ).leftMap { error =>
      if (error.getMessage.contains(s"Key (role_id)=($roleId) is not present"))
        Error("role.id", "not.found")
      else
        UnmappedError
    }.map(_ != 0)
  }

  override def findByMembership(teamId: UUID, userId: UUID): OptionT[IO, Role] = {
    OptionT(
      sql"""
            SELECT role.id, role.name
            FROM team_member_role
            INNER JOIN roles role on role.id = team_member_role.role_id
            WHERE team_id = $teamId AND user_id = $userId
            LIMIT 1
         """
        .query[Role]
        .option
        .transact(transactor)
    )
  }

  override def findMemberships(roleId: UUID): IO[List[Membership]] = {
    sql"""
          SELECT team_id, user_id
          FROM team_member_role
          WHERE role_id = $roleId
       """
      .query[Membership]
      .to[List]
      .transact(transactor)
      .attempt
      .map(_.valueOr(_ => List.empty))
  }

  override def findById(roleId: UUID): OptionT[IO, Role] = {
    OptionT(
      sql"""
            SELECT *
            FROM roles
            WHERE id = $roleId
            LIMIT 1
           """
        .query[Role]
        .option
        .transact(transactor)
    )
  }
}

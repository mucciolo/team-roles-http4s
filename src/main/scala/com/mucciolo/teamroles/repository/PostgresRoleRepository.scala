package com.mucciolo.teamroles.repository

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.implicits._
import com.mucciolo.teamroles.domain._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.util.UUID

final class PostgresRoleRepository(transactor: Transactor[IO]) extends RoleRepository {

  override def insert(roleName: String): EitherT[IO, Error, Role] = {
    EitherT(
      sql"""
            INSERT INTO roles (name)
            VALUES ($roleName)
         """
        .update
        .withUniqueGeneratedKeys[Role]("id", "name")
        .transact(transactor)
        .map(_.asRight[Error])
        .onUniqueViolation(FieldError("role.name", "already.exists").asLeft.pure[IO])
    )
  }

  override def upsertMembershipRole(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, Error, Unit] = {
    EitherT(
      sql"""
            INSERT INTO team_member_role
            VALUES ($teamId, $userId, $roleId)
            ON CONFLICT (team_id, user_id) DO UPDATE SET role_id = excluded.role_id
         """
        .update
        .run
        .transact(transactor)
        .map(_ => ().asRight[Error])
        .onForeignKeyViolation(FieldError("role.id", "not.found").asLeft.pure[IO])
    )
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

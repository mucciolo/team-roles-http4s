package com.mucciolo.teamroles.repository

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import com.mucciolo.teamroles.domain._

import java.util.UUID

trait RoleRepository {
  def insert(roleName: String): EitherT[IO, Error, Role]
  def findById(roleId: UUID): OptionT[IO, Role]
  def upsertMembershipRole(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, Error, Boolean]
  def findByMembership(teamId: UUID, userId: UUID): OptionT[IO, Role]
  def findMemberships(roleId: UUID): IO[List[Membership]]
}

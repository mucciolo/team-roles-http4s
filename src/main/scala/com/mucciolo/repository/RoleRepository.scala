package com.mucciolo.repository

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import com.mucciolo.service.Membership

import java.util.UUID

trait RoleRepository {
  def insert(roleName: String): EitherT[IO, String, Role]
  def findById(roleId: UUID): OptionT[IO, Role]
  def upsertMembershipRole(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, String, Boolean]
  def findByMembership(teamId: UUID, userId: UUID): OptionT[IO, Role]
  def findMemberships(roleId: UUID): IO[List[Membership]]
}

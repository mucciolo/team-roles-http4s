package com.mucciolo.service

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import com.mucciolo.repository._

import java.util.UUID

trait RoleService {
  def create(roleName: String): EitherT[IO, String, Role]
  def assign(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, String, Option[Boolean]]
  def roleLookup(teamId: UUID, userId: UUID): EitherT[IO, String, Role]
  def membershipLookup(roleId: UUID): OptionT[IO, List[Membership]]
}

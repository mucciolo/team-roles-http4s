package com.mucciolo.teamroles.core

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import com.mucciolo.teamroles.domain._

import java.util.UUID

trait RoleService {
  def create(roleName: String): EitherT[IO, Error, Role]
  def assign(teamId: UUID, userId: UUID, roleId: UUID): OptionT[IO, Either[Error, Unit]]
  def roleLookup(teamId: UUID, userId: UUID): OptionT[IO, Role]
  def membershipLookup(roleId: UUID): OptionT[IO, List[Membership]]
}

package com.mucciolo.service

import cats.data.EitherT
import cats.effect.IO
import com.mucciolo.repository._

import java.util.UUID

trait RoleService {
  def create(roleName: String): EitherT[IO, String, Role]
  def assign(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, String, Boolean]
  def roleLookup(teamId: UUID, userId: UUID): EitherT[IO, String, Role]
}

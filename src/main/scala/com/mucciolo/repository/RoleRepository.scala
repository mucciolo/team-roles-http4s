package com.mucciolo.repository

import cats.data.EitherT
import cats.effect.IO

trait RoleRepository {
  def insert(role: RoleInsert): EitherT[IO, String, Role]
}

package com.mucciolo.service

import cats.data.EitherT
import cats.effect.IO
import com.mucciolo.repository.{RoleInsert, Role}

trait RoleService {
  def create(role: RoleInsert): EitherT[IO, String, Role]
}

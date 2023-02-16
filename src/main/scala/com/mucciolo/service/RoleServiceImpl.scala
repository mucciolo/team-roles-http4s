package com.mucciolo.service

import cats.data.EitherT
import cats.effect.IO
import com.mucciolo.repository.{Role, RoleInsert, RoleRepository}

final class RoleServiceImpl(repository: RoleRepository) extends RoleService {

  override def create(role: RoleInsert): EitherT[IO, String, Role] = {
    repository.insert(role)
  }

}

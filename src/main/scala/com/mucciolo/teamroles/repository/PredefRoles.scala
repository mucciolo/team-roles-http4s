package com.mucciolo.teamroles.repository

import com.mucciolo.teamroles.core.Domain.Role

import java.util.UUID

object PredefRoles {
  val developer: Role = Role(
    id = UUID.fromString("328fa001-14f5-401e-877c-c80109c46417"),
    name = "Developer"
  )

  val productOwner: Role = Role(
    id = UUID.fromString("4b8e1517-76b8-411b-bb08-7b747b42f895"),
    name = "Product Owner"
  )

  val tester: Role = Role(
    id = UUID.fromString("58b229a1-7c9b-4393-bae0-096af42e85c7"),
    name = "Tester"
  )
}
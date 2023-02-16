package com.mucciolo.repository

import cats.Semigroup
import cats.data.Validated.condNec
import com.mucciolo.util.Validator
import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class Role(id: UUID, name: String)

@JsonCodec
case class RoleInsert(name: String)
object RoleInsert {
  implicit val semigroup: Semigroup[RoleInsert] = Semigroup.last[RoleInsert]

  val validator: Validator[RoleInsert] = newRole => {
    condNec(newRole.name != null, newRole, "Role name must not be empty") andThen { _ =>
      condNec(!newRole.name.isBlank, newRole, "Role name must not be blank") combine
        condNec(newRole.name.length <= 100, newRole, "Role name must no be longer than 100 characters") combine
        condNec(newRole.name.forall(c => c.isLetter || c.isSpaceChar), newRole, "Role name contains invalid characters")
    }

  }
}

package com.mucciolo.teamroles.routes

import cats.Semigroup
import cats.data.NonEmptyChain
import cats.data.Validated.condNec
import com.mucciolo.teamroles.util.Validator
import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class Error(errors: NonEmptyChain[String])
object Error {
  def apply(error: String): Error = Error(NonEmptyChain.one(error))
}

@JsonCodec
case class RoleCreationRequest(name: Option[String])
object RoleCreationRequest {
  implicit val semigroup: Semigroup[RoleCreationRequest] = Semigroup.last

  val validator: Validator[RoleCreationRequest] = newRole => {
    condNec(newRole.name.nonEmpty, newRole, "Missing role name") andThen { _ =>
      condNec(newRole.name.get.trim.nonEmpty, newRole, "Role name must be nonempty") combine
        condNec(newRole.name.get.length <= 100, newRole, "Role name must have at maximum 100 characters") combine
        condNec(newRole.name.get.forall(c => c.isLetter || c.isSpaceChar), newRole, "Role name contains invalid characters")
    }
  }
}

@JsonCodec
case class RoleAssignmentRequest(roleId: Option[UUID])
object RoleAssignmentRequest {
  implicit val semigroup: Semigroup[RoleAssignmentRequest] = Semigroup.last

  val validator: Validator[RoleAssignmentRequest] = assignment => {
    condNec(assignment.roleId.nonEmpty, assignment, "Missing role id")
  }
}
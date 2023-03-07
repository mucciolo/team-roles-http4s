package com.mucciolo.teamroles.routes

import cats.Semigroup
import cats.data.NonEmptyChain
import cats.data.Validated.condNec
import cats.implicits._
import com.mucciolo.teamroles.util.Validator
import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
final case class ValidationError(errors: NonEmptyChain[String])
object ValidationError {
  def apply(error: String): ValidationError = ValidationError(NonEmptyChain.one(error))
}

@JsonCodec
final case class RoleCreationRequest(name: Option[String])
object RoleCreationRequest {
  implicit val semigroup: Semigroup[RoleCreationRequest] = Semigroup.last

  val validator: Validator[RoleCreationRequest] = newRole => {
    condNec(newRole.name.nonEmpty, newRole, "Missing role name") andThen { _ =>
      condNec(newRole.name.get.trim.nonEmpty, newRole, "Role name must be nonempty") |+|
        condNec(newRole.name.get.length <= 100, newRole, "Role name must have at maximum 100 characters") |+|
        condNec(newRole.name.get.forall(c => c.isLetter || c.isSpaceChar), newRole, "Role name contains invalid characters")
    }
  }
}

@JsonCodec
final case class RoleAssignmentRequest(roleId: Option[UUID])
object RoleAssignmentRequest {
  implicit val semigroup: Semigroup[RoleAssignmentRequest] = Semigroup.last

  val validator: Validator[RoleAssignmentRequest] = assignment => {
    condNec(assignment.roleId.nonEmpty, assignment, "Missing role id")
  }
}
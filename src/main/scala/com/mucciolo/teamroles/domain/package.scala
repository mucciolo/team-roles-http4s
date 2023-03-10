package com.mucciolo.teamroles

import cats.data.NonEmptyChain
import cats.data.Validated.condNec
import cats.kernel.Semigroup
import cats.syntax.functor._
import com.mucciolo.teamroles.util.Validator
import io.circe.generic.JsonCodec
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import java.util.UUID

package object domain {

  type RoleName = String

  @JsonCodec
  final case class Role(id: UUID, name: RoleName)
  object Role {
    object Predef {
      val Developer: Role = Role(
        id = UUID.fromString("328fa001-14f5-401e-877c-c80109c46417"),
        name = "Developer"
      )

      val ProductOwner: Role = Role(
        id = UUID.fromString("4b8e1517-76b8-411b-bb08-7b747b42f895"),
        name = "Product Owner"
      )

      val Tester: Role = Role(
        id = UUID.fromString("58b229a1-7c9b-4393-bae0-096af42e85c7"),
        name = "Tester"
      )
    }
  }

  @JsonCodec
  final case class Membership(teamId: UUID, userId: UUID)

  sealed trait Error
  object Error {
    implicit val encodeError: Encoder[Error] = Encoder.instance {
      case fieldError: FieldError => fieldError.asJson
      case validationError: ValidationError => validationError.asJson
    }

    implicit val decodeError: Decoder[Error] =
      List[Decoder[Error]](
        Decoder[FieldError].widen,
        Decoder[ValidationError].widen,
      ).reduceLeft(_ or _)
  }

  @JsonCodec
  final case class FieldError(field: String, message: String) extends Error

  @JsonCodec
  final case class ValidationError(errors: NonEmptyChain[FieldError]) extends Error

  object Validators {
    val roleNameValidator: Validator[RoleName] = {
      implicit val stringSemigroup: Semigroup[String] = Semigroup.last
      roleName => {
        condNec(roleName.trim.nonEmpty, roleName, FieldError("role.name", "empty")) combine
          condNec(roleName.length <= 100, roleName, FieldError("role.name", "too.long")) combine
          condNec(roleName.forall(c => c.isLetter || c.isSpaceChar), roleName, FieldError("role.name", "invalid.chars"))
      }
    }
  }

}

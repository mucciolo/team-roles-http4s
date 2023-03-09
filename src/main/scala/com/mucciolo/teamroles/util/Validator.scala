package com.mucciolo.teamroles.util

import cats.data.ValidatedNec
import cats.implicits._
import com.mucciolo.teamroles.domain._

trait Validator[A] {
  protected def validated(instance: A): ValidatedNec[FieldError, A]
  final def validate(instance: A): Either[ValidationError, A] = {
    validated(instance).toEither.leftMap(ValidationError.apply)
  }
}

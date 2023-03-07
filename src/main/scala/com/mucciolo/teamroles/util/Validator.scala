package com.mucciolo.teamroles.util

import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import io.circe.generic.JsonCodec

trait Validator[A] {
  protected def validated(instance: A): ValidatedNec[String, A]
  final def validate(instance: A): Either[ValidationError, A] = {
    validated(instance).toEither.leftMap(ValidationError(_))
  }
}

@JsonCodec
final case class ValidationError(errors: NonEmptyChain[String])
object ValidationError {
  def apply(error: String): ValidationError = ValidationError(NonEmptyChain.one(error))
}

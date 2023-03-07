package com.mucciolo.teamroles.util

import cats.data.ValidatedNec

trait Validator[A] {
  def validate(instance: A): ValidatedNec[String, A]
}

package com.mucciolo.routes

import cats.data.NonEmptyChain
import io.circe.generic.JsonCodec

@JsonCodec
case class Error(errors: NonEmptyChain[String])
object Error {
  def apply(error: String): Error = Error(NonEmptyChain.one(error))
}

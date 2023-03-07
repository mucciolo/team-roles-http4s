package com.mucciolo.teamroles.core

import io.circe.generic.JsonCodec

import java.util.UUID

object Domain {

  @JsonCodec
  final case class Role(id: UUID, name: String)

  @JsonCodec
  final case class Membership(teamId: UUID, userId: UUID)

  @JsonCodec
  final case class Error(field: String, message: String)

}

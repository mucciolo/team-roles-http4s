package com.mucciolo.teamroles.core

import io.circe.generic.JsonCodec

import java.util.UUID

object Domain {

  @JsonCodec
  case class Role(id: UUID, name: String)

  @JsonCodec
  case class Membership(teamId: UUID, userId: UUID)
}

package com.mucciolo.teamroles.routes

import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec(decodeOnly = true)
final case class RoleCreationRequest(name: String)

@JsonCodec(decodeOnly = true)
final case class RoleAssignmentRequest(roleId: UUID)
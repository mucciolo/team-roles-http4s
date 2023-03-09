package com.mucciolo.teamroles

import io.circe.generic.JsonCodec

import java.util.UUID

package object userteams {
  @JsonCodec
  case class Team(id: UUID, name: String, teamLeadId: UUID, teamMemberIds: Set[UUID])
}

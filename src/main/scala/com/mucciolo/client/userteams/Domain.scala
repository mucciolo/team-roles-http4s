package com.mucciolo.client.userteams

import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class User(id: UUID, firstName: String, lastName: String)

@JsonCodec
case class Team(id: UUID, name: String, teamLeadId: UUID, teamMemberIds: Set[UUID])

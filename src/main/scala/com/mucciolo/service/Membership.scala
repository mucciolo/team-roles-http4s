package com.mucciolo.service

import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class Membership(teamId: UUID, userId: UUID)

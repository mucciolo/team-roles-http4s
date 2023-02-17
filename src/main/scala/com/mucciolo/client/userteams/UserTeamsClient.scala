package com.mucciolo.client.userteams

import cats.data.OptionT
import cats.effect.IO

import java.util.UUID

trait UserTeamsClient {
  def findTeamById(id: UUID): OptionT[IO, Team]
}

package com.mucciolo.client.userteams

import cats.effect.IO

import java.util.UUID

trait UserTeamsClient {
  def findTeamById(id: UUID): IO[Option[Team]]
}

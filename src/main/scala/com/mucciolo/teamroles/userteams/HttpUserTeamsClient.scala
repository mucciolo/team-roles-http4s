package com.mucciolo.teamroles.userteams

import cats.data.OptionT
import cats.effect.IO
import com.mucciolo.teamroles.config.UserTeamsClientConf
import org.http4s.client.Client
import org.http4s.{EntityDecoder, circe}

import java.util.UUID

final class HttpUserTeamsClient (
  client: Client[IO], conf: UserTeamsClientConf
) extends UserTeamsClient {

  private implicit val teamOptDecoder: EntityDecoder[IO, Option[Team]] =
    circe.jsonOf[IO, Option[Team]]

  override def findTeamById(id: UUID): OptionT[IO, Team] =
    OptionT(client.expect[Option[Team]](conf.origin / "teams" / id))

}

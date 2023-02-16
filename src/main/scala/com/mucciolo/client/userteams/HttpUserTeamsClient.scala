package com.mucciolo.client.userteams

import cats.effect.IO
import com.mucciolo.config.UserTeamsClientConf
import org.http4s.Method.GET
import org.http4s.{EntityDecoder, Request, circe}
import org.http4s.client.Client

import java.util.UUID

final class HttpUserTeamsClient (
  client: Client[IO], conf: UserTeamsClientConf
) extends UserTeamsClient {

  private implicit val teamOptDecoder: EntityDecoder[IO, Option[Team]] =
    circe.jsonOf[IO, Option[Team]]

  override def findTeamById(id: UUID): IO[Option[Team]] = {
    val request = Request[IO](
      method = GET,
      uri = conf.origin / "teams" / id
    )

    client.expect[Option[Team]](request)
  }

}

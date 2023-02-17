package com.mucciolo.client

import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import cats.effect.{IO, Resource}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.mucciolo.client.userteams.{HttpUserTeamsClient, Team}
import com.mucciolo.config.UserTeamsClientConf
import io.circe.syntax.EncoderOps
import org.http4s.Uri
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

final class HttpUserTeamsClientSpec extends FixtureAsyncFreeSpec with Matchers with BeforeAndAfterAll
  with AsyncIOSpec with CatsResourceIO[HttpUserTeamsClient] {

  private val wireMockServer = new WireMockServer(8089)
  override val resource: Resource[IO, HttpUserTeamsClient] =
    EmberClientBuilder
      .default[IO]
      .build
      .map { httpClient =>
        val conf = UserTeamsClientConf(origin = Uri.unsafeFromString(wireMockServer.baseUrl()))
        new HttpUserTeamsClient(httpClient, conf)
      }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  "HttpUserTeamsClient" - {
    "should return some team given existing team id" in { client =>

      val teamId = UUID.fromString("37484ca1-633d-4462-9cc3-33f07f6b31e8")
      val team = Team(
        id = teamId,
        name = "Test Team",
        teamLeadId = UUID.fromString("0382404a-46ef-430e-98a4-51cf8016dfa9"),
        teamMemberIds = Set(
          UUID.fromString("70b72354-515f-4dac-831f-fb2ff3191d61"),
          UUID.fromString("0a2676ba-8073-48cc-b445-2b41bace256d")
        )
      )

      wireMockServer.stubFor(
        get(urlPathEqualTo(s"/teams/$teamId"))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(team.asJson.spaces2)
            .withStatus(200)
          )
      )

      client.findTeamById(teamId).value.asserting(_ shouldBe Some(team))

    }

    "should return none given non-existent team id" in { client =>

      val teamId = UUID.fromString("00dce9a9-c766-4a2d-8487-b5cc65d59be1")

      wireMockServer.stubFor(
        get(urlPathEqualTo(s"/teams/$teamId"))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("null")
            .withStatus(200)
          )
      )

      client.findTeamById(teamId).value.asserting(_ shouldBe None)

    }
  }

}

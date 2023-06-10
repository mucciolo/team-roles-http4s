package com.mucciolo.teamroles.client

import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import cats.effect.{IO, Resource}
import com.github.tomakehurst.wiremock.WireMockServer
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.should.Matchers

trait HttpClientIntegrationTest[C] extends FixtureAsyncFreeSpec with Matchers with BeforeAndAfterAll
  with AsyncIOSpec with CatsResourceIO[C] {

  protected def createClient(wireMockServerBaseUrl: String, httpClient: Client[IO]): C

  protected val wireMockServer = new WireMockServer()
  override val resource: Resource[IO, C] =
    EmberClientBuilder.default[IO].build.map(createClient(wireMockServer.baseUrl(), _))

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

}

package com.mucciolo.teamroles.routes

import cats.data._
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.catsSyntaxEitherId
import com.mucciolo.teamroles.domain._
import com.mucciolo.teamroles.core.RoleService
import io.circe.Json
import io.circe.Json.fromString
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Request, Response, Status}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.util.UUID

final class RoleRoutesSpec extends AsyncWordSpec with AsyncIOSpec with AsyncMockFactory
  with Matchers {

  private val service = mock[RoleService]
  private val routes: HttpRoutes[IO] = RoleRoutes(service)

  private val teamId = UUID.fromString("dcb537df-3ac9-40cb-adcb-b15f652f18f7")
  private val userId = UUID.fromString("3a05f9ce-3873-4de4-a8ce-6a15e84158cc")
  private val roleId = UUID.fromString("ff80826c-41de-4462-92cf-5967d0f3324f")

  private def send(routes: HttpRoutes[IO])(request: Request[IO]): IO[Response[IO]] = {
    routes.orNotFound(request)
  }

  private def roleCreationRequest(role: Json) = {
    Request[IO](POST, uri"/teams/roles").withEntity(role)
  }

  "POST /teams/roles" when {
    "given valid request" should {
      "return 201 with newly created object" in {

        val roleName = "New Role"
        val role: Json = Json.obj(
          "name" -> fromString(roleName)
        )
        val expectedCreatedRole = Role(
          id = UUID.fromString("b93558f5-0ac8-47f8-8cf0-7b131364d464"),
          name = roleName
        )

        service.create _ expects roleName returns EitherT.rightT(expectedCreatedRole)

        val request = roleCreationRequest(role)
        val response = send(routes)(request)

        response.flatMap { res =>
          res.status shouldBe Status.Created
          res.as[Role].asserting(_ shouldBe expectedCreatedRole)
        }
      }
    }

    "given invalid request" should {

      "return 400 given missing role name" in {

        val invalidRole: Json = Json.obj()

        val request = roleCreationRequest(invalidRole)
        val response = send(routes)(request)

        response.asserting(_.status shouldBe Status.UnprocessableEntity)
      }

      "return 400 given null role name" in {

        val invalidRole: Json = Json.obj(
          "name" -> Json.Null
        )

        val request = roleCreationRequest(invalidRole)
        val response = send(routes)(request)

        response.asserting(_.status shouldBe Status.UnprocessableEntity)
      }

      "return 400 given role creation error" in {
        val roleName = "New Role"
        val role: Json = Json.obj(
          "name" -> fromString(roleName)
        )
        val error = FieldError("*", "test")
        service.create _ expects roleName returns EitherT.leftT(error)

        val request = roleCreationRequest(role)
        val response = send(routes)(request)

        response.flatMap { res =>
          res.status shouldBe Status.BadRequest
          res.as[FieldError].asserting(_ shouldBe error)
        }
      }
    }

  }

  private def roleAssignmentRequest(teamId: UUID, userId: UUID, assignment: Json) = {
    Request[IO](PUT, uri"/teams" / teamId / "members" / userId / "role").withEntity(assignment)
  }

  "PUT /teams/:teamId/members/:userId/role" when {

    "given team exists and user is a member" should {
      "return 204" in {

        val assignment: Json = Json.obj(
          "roleId" -> fromString(roleId.toString)
        )
        service.assign _ expects (teamId, userId, roleId) returns OptionT.some(Right(true))

        val request = roleAssignmentRequest(teamId, userId, assignment)
        val response = send(routes)(request)

        response.map(_.status shouldBe Status.NoContent)
      }
    }

    "given missing role id" should {
      "return 400" in {
        val invalidAssignment: Json = Json.obj()

        val request = roleAssignmentRequest(teamId, userId, invalidAssignment)
        val response = send(routes)(request)

        response.asserting(_.status shouldBe Status.UnprocessableEntity)
      }
    }

    "given null role id" should {
      "return 400" in {
        val invalidAssignment: Json = Json.obj(
          "roleId" -> Json.Null
        )

        val request = roleAssignmentRequest(teamId, userId, invalidAssignment)
        val response = send(routes)(request)

        response.asserting(_.status shouldBe Status.UnprocessableEntity)
      }
    }


    "given role assignment error" should {
      "return 400" in {

        val assignment: Json = Json.obj(
          "roleId" -> fromString(roleId.toString)
        )
        val error = FieldError("*", "test")
        service.assign _ expects (teamId, userId, roleId) returns OptionT.some(Left(error))

        val request = roleAssignmentRequest(teamId, userId, assignment)
        val response = send(routes)(request)

        response.flatMap { res =>
          res.status shouldBe Status.BadRequest
          res.as[FieldError].asserting(_ shouldBe error)
        }

      }
    }

    "given role does not exist" should {
      "return 404" in {

        val assignment: Json = Json.obj(
          "roleId" -> fromString(roleId.toString)
        )
        service.assign _ expects (teamId, userId, roleId) returns OptionT.none

        val request = roleAssignmentRequest(teamId, userId, assignment)
        val response = send(routes)(request)

        response.asserting(_.status shouldBe Status.NotFound)
      }
    }

  }

  private def roleLookupRequest(teamId: UUID, userId: UUID) = {
    Request[IO](GET, uri"/teams" / teamId / "members" / userId / "role")
  }

  "GET /teams/:teamId/members/:userId/role" when {
    "given team exists and user is a member" should {
      "return 200 with the assigned role" in {

        val assignedRole = Role(
          id = UUID.fromString("06e5800d-598e-48b8-890a-02d6e61fab6b"),
          name = "Assigned Role"
        )

        service.roleLookup _ expects (teamId, userId) returns OptionT.some(assignedRole)

        val request = roleLookupRequest(teamId, userId)
        val response = send(routes)(request)

        response.flatMap { res =>
          res.status shouldBe Status.Ok
          res.as[Role].asserting(_ shouldBe assignedRole)
        }
      }
    }

    "membership is not found" should {
      "return 404" in {

        service.roleLookup _ expects (teamId, userId) returns OptionT.none

        val request = roleLookupRequest(teamId, userId)
        val response = send(routes)(request)

        response.asserting(_.status shouldBe Status.NotFound)

      }
    }
  }

  private def membershipLookupRequest(roleId: UUID) = {
    Request[IO](GET, uri"/roles" / roleId / "assignments")
  }

  "GET /roles/:roleId/assignments" when {
    "given existing role" should {
      "return 200 with memberships" in {

        val memberships = List(
          Membership(
            UUID.fromString("fb236e8f-d5da-494f-9b19-26c516d40d88"),
            UUID.fromString("be4546a8-1952-4864-97ad-1d14595b209e")
          ),
          Membership(
            UUID.fromString("d00f591f-6556-47a4-8894-3834fd921468"),
            UUID.fromString("035377a0-dc70-4f40-a617-192caf47ad6b")
          )
        )

        service.membershipLookup _ expects roleId returns OptionT.pure(memberships)

        val request = membershipLookupRequest(roleId)
        val response = send(routes)(request)

        response.flatMap { res =>
          res.status shouldBe Status.Ok
          res.as[List[Membership]].asserting(_ shouldBe memberships)
        }
      }
    }

    "given role does not exist" should {
      "should return 404" in {

        service.membershipLookup _ expects roleId returns OptionT.none

        val request = membershipLookupRequest(roleId)
        val response = send(routes)(request)

        response.asserting { res =>
          res.status shouldBe Status.NotFound
        }

      }
    }
  }

}

package com.mucciolo.routes

import cats.data.Validated._
import cats.data._
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.catsSyntaxEitherId
import com.mucciolo.repository._
import com.mucciolo.service.{Membership, RoleService}
import com.mucciolo.util.Validator
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Request, Response, Status}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

final class RoleRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with AsyncMockFactory
  with Matchers {

  private val service = stub[RoleService]
  private val defaultRoutes: HttpRoutes[IO] = RoleRoutes(service)
  private val teamId = UUID.fromString("dcb537df-3ac9-40cb-adcb-b15f652f18f7")
  private val userId = UUID.fromString("3a05f9ce-3873-4de4-a8ce-6a15e84158cc")
  private val roleId = UUID.fromString("ff80826c-41de-4462-92cf-5967d0f3324f")

  private def send(routes: HttpRoutes[IO])(request: Request[IO]): IO[Response[IO]] = {
    routes.orNotFound(request)
  }

  "Role creation" - {

    "should return 201 with newly created object given valid request" in {

      val roleName = "New Role"
      val role = RoleCreationRequest(Some(roleName))
      val expectedCreatedRole = Role(
        id = UUID.fromString("b93558f5-0ac8-47f8-8cf0-7b131364d464"),
        name = roleName
      )

      service.create _ when roleName returns EitherT.rightT(expectedCreatedRole)

      val request = roleCreationRequest(role)
      val response = send(defaultRoutes)(request)

      response.flatMap { res =>
        res.status shouldBe Status.Created
        res.as[Role].asserting(_ shouldBe expectedCreatedRole)
      }
    }

    "should return 400 with errors" - {

      "given invalid request" in {

        val invalidRole = RoleCreationRequest(name = None)
        val invalidator: Validator[RoleCreationRequest] = _ => invalidNec("Invalid role name")
        val routes = RoleRoutes(service, invalidator)

        val request = roleCreationRequest(invalidRole)
        val response = send(routes)(request)
        val expectedError = Error("Invalid role name")

        response.flatMap { res =>
          res.status shouldBe Status.BadRequest
          res.as[Error].asserting(_ shouldBe expectedError)
        }
      }

      "given role creation error" in {

        val roleName = "New Role"
        val role = RoleCreationRequest(Some(roleName))
         service.create _ when roleName returns EitherT.leftT("Something happened")

        val request = roleCreationRequest(role)
        val response = send(defaultRoutes)(request)
        val expectedError = Error("Something happened")

        response.flatMap { res =>
          res.status shouldBe Status.BadRequest
          res.as[Error].asserting(_ shouldBe expectedError)
        }
      }
    }

  }

  private def roleCreationRequest(role: RoleCreationRequest) = {
    Request[IO](POST, uri"/teams/roles").withEntity(role)
  }

  "Role assignment" - {

    "should return 204 given team exists and user is a member" in {

      val assignment = RoleAssignmentRequest(Some(roleId))
      service.assign _ when (teamId, userId, roleId) returns EitherT.rightT(Some(true))

      val request = roleAssignmentRequest(teamId, userId, assignment)
      val response = send(defaultRoutes)(request)

      response.map { res =>
        res.status shouldBe Status.NoContent
      }
    }

    "should return 400 with errors" - {

      "given invalid request" in {

        val assignment = RoleAssignmentRequest(None)
        val invalidator: Validator[RoleAssignmentRequest] = _ => invalidNec("Missing role id")
        val routes = RoleRoutes(service, assignmentValidator = invalidator)

        val request = roleAssignmentRequest(teamId, userId, assignment)
        val response = send(routes)(request)
        val expectedError = Error("Missing role id")

        response.flatMap { res =>
          res.status shouldBe Status.BadRequest
          res.as[Error].asserting(_ shouldBe expectedError)
        }

      }

      "given role assignment error" in {

        val assignment = RoleAssignmentRequest(Some(roleId))
        service.assign _ when (teamId, userId, roleId) returns EitherT.leftT("Something happened")

        val request = roleAssignmentRequest(teamId, userId, assignment)
        val response = send(defaultRoutes)(request)
        val expectedError = Error("Something happened")

        response.flatMap { res =>
          res.status shouldBe Status.BadRequest
          res.as[Error].asserting(_ shouldBe expectedError)
        }

      }
    }

    "should return 404 given role does not exist" in {

      val assignment = RoleAssignmentRequest(Some(roleId))

      service.assign _ when (teamId, userId, roleId) returns EitherT.rightT(None)

      val request = roleAssignmentRequest(teamId, userId, assignment)
      val response = send(defaultRoutes)(request)

      response.asserting { res =>
        res.status shouldBe Status.NotFound
      }

    }

  }

  private def roleAssignmentRequest(teamId: UUID, userId: UUID, assignment: RoleAssignmentRequest) = {
    Request[IO](PUT, uri"/teams" / teamId / "members" / userId / "role").withEntity(assignment)
  }

  "Role lookup" - {
    "should return 200 with the assigned role given team exists and user is a member" in {

      val assignedRole = Role(
        id = UUID.fromString("06e5800d-598e-48b8-890a-02d6e61fab6b"),
        name = "Assigned Role"
      )

      service.roleLookup _ when (teamId, userId) returns EitherT.pure(assignedRole)

      val request = roleLookupRequest(teamId, userId)
      val response = send(defaultRoutes)(request)

      response.flatMap { res =>
        res.status shouldBe Status.Ok
        res.as[Role].asserting(_ shouldBe assignedRole)
      }
    }

    "should return 400 with errors role lookup error" in {

      service.roleLookup _ when (teamId, userId) returns EitherT.leftT("Something happened")

      val request = roleLookupRequest(teamId, userId)
      val response = send(defaultRoutes)(request)
      val expectedError = Error("Something happened")

      response.flatMap { res =>
        res.status shouldBe Status.BadRequest
        res.as[Error].asserting(_ shouldBe expectedError)
      }

    }
  }

  private def roleLookupRequest(teamId: UUID, userId: UUID) = {
    Request[IO](GET, uri"/teams" / teamId / "members" / userId / "role")
  }

  "Membership lookup by role" - {
    "should return 200 with memberships given existing role" in {

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

      service.membershipLookup _ when roleId returns OptionT.pure(memberships)

      val request = membershipLookupRequest(roleId)
      val response = send(defaultRoutes)(request)

      response.flatMap { res =>
        res.status shouldBe Status.Ok
        res.as[List[Membership]].asserting(_ shouldBe memberships)
      }
    }

    "should return 404 given role does not exist" in {

      service.membershipLookup _ when roleId returns OptionT.none

      val request = membershipLookupRequest(roleId)
      val response = send(defaultRoutes)(request)

      response.asserting { res =>
        res.status shouldBe Status.NotFound
      }

    }
  }

  private def membershipLookupRequest(roleId: UUID) = {
    Request[IO](GET, uri"/roles" / roleId / "assignments")
  }

}

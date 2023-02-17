package com.mucciolo.service

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.mucciolo.client.userteams.{Team, UserTeamsClient}
import com.mucciolo.repository._
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.EitherValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

final class RoleServiceImplSpec extends AsyncFreeSpec with AsyncIOSpec
  with AsyncMockFactory with Matchers with EitherValues {

  private val teamId = UUID.fromString("5bd70a66-f6a0-42fc-8bdb-6c40841fab62")
  private val userId = UUID.fromString("ef2d1c0b-7ddb-446d-80d4-77301cbd4ffa")
  private val role = PredefRoles.developer

  private val repository = stub[RoleRepository]
  private val userTeamsClient = stub[UserTeamsClient]
  private val service = new RoleServiceImpl(repository, userTeamsClient)

  "RoleServiceImpl" - {

    "create" - {
      "should normalize role name spaces" in {

        val roleName = " Role  Name   "
        val normalizedRoleName = "Role Name"
        val normalizedRole = Role(
          id = UUID.fromString("79446410-73ad-4122-8110-0904974c2738"),
          name = normalizedRoleName
        )

        repository.insert _ when normalizedRoleName returns EitherT.pure(normalizedRole)

        service.create(roleName)
          .value
          .asserting(_.value shouldBe normalizedRole)
      }
    }

    "assign" - {

      "given role does not exists" - {
        "should return error" in {

          repository.findById _ when role.id returns OptionT.none

          service.assign(teamId, userId, role.id)
            .value
            .asserting(_.value shouldBe None)

        }
      }

      "given existing role" - {
        "should return error given nonexistent team" in {

          repository.findById _ when role.id returns OptionT.pure(role)
          userTeamsClient.findTeamById _ when teamId returns IO.pure(None)

          service.assign(teamId, userId, role.id)
            .value
            .asserting(_.left.value shouldBe "Team not found")

        }

        "should return error given user is not a team member" in {

          val team = Team(
            id = teamId,
            name = "Not a team member",
            teamLeadId = UUID.fromString("68375515-cd6f-4fd7-963e-277de152f9c1"),
            teamMemberIds = Set(
              UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
              UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6")
            )
          )

          repository.findById _ when role.id returns OptionT.pure(role)
          userTeamsClient.findTeamById _ when teamId returns IO.pure(Some(team))

          service.assign(teamId, userId, role.id)
            .value
            .asserting(_.left.value shouldBe "User is not a team member")

        }

        "should return true given user is team lead" in {

          val team = Team(
            id = teamId,
            name = "User is not team lead",
            teamLeadId = userId,
            teamMemberIds = Set(
              UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
              UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6")
            )
          )

          repository.findById _ when role.id returns OptionT.pure(role)
          repository.upsertMembershipRole _ when (teamId, userId, role.id) returns EitherT.rightT(true)
          userTeamsClient.findTeamById _ when teamId returns IO.pure(Some(team))

          service.assign(teamId, userId, role.id)
            .value
            .asserting(_.value shouldBe Some(true))

        }

        "should return true given user is a team member" in {

          val team = Team(
            id = teamId,
            name = "User is not team lead",
            teamLeadId = UUID.fromString("68375515-cd6f-4fd7-963e-277de152f9c1"),
            teamMemberIds = Set(
              UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
              UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6"),
              userId
            )
          )

          repository.findById _ when role.id returns OptionT.pure(role)
          repository.upsertMembershipRole _ when (teamId, userId, role.id) returns EitherT.rightT(true)
          userTeamsClient.findTeamById _ when teamId returns IO.pure(Some(team))

          service.assign(teamId, userId, role.id)
            .value
            .asserting(_.value shouldBe Some(true))

        }
      }

    }

    "roleLookup" - {
      "should return error given nonexistent team" in {

        userTeamsClient.findTeamById _ when teamId returns IO.pure(None)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.left.value shouldBe "Team not found")

      }

      "should return error given user is not a team member" in {

        val team = Team(
          id = teamId,
          name = "Not a team member",
          teamLeadId = UUID.fromString("68375515-cd6f-4fd7-963e-277de152f9c1"),
          teamMemberIds = Set(
            UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
            UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6")
          )
        )

        repository.findById _ when role.id returns OptionT.pure(role)
        userTeamsClient.findTeamById _ when teamId returns IO.pure(Some(team))

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.left.value shouldBe "User is not a team member")

      }

      "should return Developer role given user has not been assigned a role" in {

        val team = Team(
          id = teamId,
          name = "Nonexistent role",
          teamLeadId = UUID.fromString("68375515-cd6f-4fd7-963e-277de152f9c1"),
          teamMemberIds = Set(
            UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
            UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6"),
            userId
          )
        )

        userTeamsClient.findTeamById _ when teamId returns IO.pure(Some(team))
        repository.findByMembership _ when (teamId, userId) returns OptionT.none

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.value shouldBe PredefRoles.developer)

      }

      "should return assigned role given user has a role assigned" in {

        val team = Team(
          id = teamId,
          name = "Nonexistent role",
          teamLeadId = UUID.fromString("68375515-cd6f-4fd7-963e-277de152f9c1"),
          teamMemberIds = Set(
            UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
            UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6"),
            userId
          )
        )
        val assignedRole = Role(
          id = UUID.fromString("eef9baae-7a77-4389-b1d5-e3e544165817"),
          name = "Assigned Role"
        )

        userTeamsClient.findTeamById _ when teamId returns IO.pure(Some(team))
        repository.findByMembership _ when (teamId, userId) returns OptionT.pure(assignedRole)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.value shouldBe assignedRole)

      }

    }
  }
}

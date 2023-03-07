package com.mucciolo.teamroles.core

import cats.data.{EitherT, OptionT}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.mucciolo.teamroles.repository.{PredefRoles, RoleRepository}
import com.mucciolo.teamroles.userteams.{Team, UserTeamsClient}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.EitherValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.mucciolo.teamroles.core.Domain._

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

      "should return error given repository error" in {

        val team = Team(
          id = teamId,
          name = "User is a team member but not the lead",
          teamLeadId = UUID.fromString("af4c850f-b911-4ad2-a692-93600d6e8461"),
          teamMemberIds = Set(
            UUID.fromString("abbeca3b-5ad6-4a55-8652-6f599a93d632"),
            UUID.fromString("f74a61e9-a618-4c43-9b31-7de524423186"),
            userId
          )
        )

        repository.upsertMembershipRole _ when (teamId, userId, role.id) returns EitherT.leftT("Error")
        userTeamsClient.findTeamById _ when teamId returns OptionT.some(team)

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_.left.value shouldBe "Error")

      }

      "should return error given nonexistent team" in {

        userTeamsClient.findTeamById _ when teamId returns OptionT.none

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_.left.value shouldBe "Team not found")

      }

      "should return error given user is not a team member or lead" in {

        val team = Team(
          id = teamId,
          name = "Not a team member",
          teamLeadId = UUID.fromString("68375515-cd6f-4fd7-963e-277de152f9c1"),
          teamMemberIds = Set(
            UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
            UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6")
          )
        )

        userTeamsClient.findTeamById _ when teamId returns OptionT.some(team)

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
            UUID.fromString("4e52b2a4-bb8a-46df-b62d-faa94607129e"),
            UUID.fromString("6d772951-d4d3-4132-8798-0df50364f790")
          )
        )

        repository.upsertMembershipRole _ when (teamId, userId, role.id) returns EitherT.rightT(true)
        userTeamsClient.findTeamById _ when teamId returns OptionT.some(team)

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_.value shouldBe Some(true))

      }

      "should return true given user is a team member" in {

        val team = Team(
          id = teamId,
          name = "User is a team member but not the lead",
          teamLeadId = UUID.fromString("af4c850f-b911-4ad2-a692-93600d6e8461"),
          teamMemberIds = Set(
            UUID.fromString("abbeca3b-5ad6-4a55-8652-6f599a93d632"),
            UUID.fromString("f74a61e9-a618-4c43-9b31-7de524423186"),
            userId
          )
        )

        repository.upsertMembershipRole _ when (teamId, userId, role.id) returns EitherT.rightT(true)
        userTeamsClient.findTeamById _ when teamId returns OptionT.some(team)

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_.value shouldBe Some(true))

      }

    }

    "roleLookup" - {
      "should return error given nonexistent team" in {

        repository.findByMembership _ when (teamId, userId) returns OptionT.none
        userTeamsClient.findTeamById _ when teamId returns OptionT.none

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.left.value shouldBe "Team not found")

      }

      "should return error given user is not a team member" in {

        val team = Team(
          id = teamId,
          name = "Not a team member",
          teamLeadId = UUID.fromString("192054bd-436f-48d9-86ba-4aa86fcdb7de"),
          teamMemberIds = Set(
            UUID.fromString("3f229bb4-eec1-4c95-93e5-79c902eb7a40"),
            UUID.fromString("667d5d04-e216-41fa-85c0-51a187ec0a45")
          )
        )

        repository.findById _ when role.id returns OptionT.pure(role)
        repository.findByMembership _ when (teamId, userId) returns OptionT.none
        userTeamsClient.findTeamById _ when teamId returns OptionT.some(team)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.left.value shouldBe "User is not a team member")

      }

      "should return Developer role given user has not been assigned a role" in {

        val team = Team(
          id = teamId,
          name = "Nonexistent role",
          teamLeadId = UUID.fromString("8c63946b-9bf1-47fb-ac5b-421eec86c807"),
          teamMemberIds = Set(
            UUID.fromString("f4515ada-e106-4702-94a5-e8a43249db7a"),
            UUID.fromString("cd9dc074-97d1-4bcc-8d62-c81c67592b8a"),
            userId
          )
        )

        repository.findByMembership _ when (teamId, userId) returns OptionT.none
        userTeamsClient.findTeamById _ when teamId returns OptionT.some(team)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.value shouldBe PredefRoles.developer)

      }

      "should return assigned role given user has a role assigned" in {

        val team = Team(
          id = teamId,
          name = "Nonexistent role",
          teamLeadId = UUID.fromString("b083263a-f63a-496c-ae10-8de0eca8e51c"),
          teamMemberIds = Set(
            UUID.fromString("65003f1b-e38e-4646-b70d-b7b734d6689f"),
            UUID.fromString("43967b63-a6bc-4060-ae74-985a48654fdb"),
            userId
          )
        )
        val assignedRole = Role(
          id = UUID.fromString("21eaaefe-25c3-4ca0-b45c-38b559033876"),
          name = "Assigned Role"
        )

        userTeamsClient.findTeamById _ when teamId returns OptionT.some(team)
        repository.findByMembership _ when (teamId, userId) returns OptionT.pure(assignedRole)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_.value shouldBe assignedRole)

      }

    }
  }
}

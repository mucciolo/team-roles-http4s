package com.mucciolo.teamroles.core

import cats.data.{EitherT, OptionT}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.mucciolo.teamroles.domain._
import com.mucciolo.teamroles.repository.RoleRepository
import com.mucciolo.teamroles.userteams.{Team, UserTeamsClient}
import org.apache.commons.lang3.StringUtils.repeat
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.util.UUID

final class RoleServiceImplSpec extends AsyncWordSpec with AsyncIOSpec
  with AsyncMockFactory with Matchers with EitherValues {

  private val teamId = UUID.fromString("5bd70a66-f6a0-42fc-8bdb-6c40841fab62")
  private val userId = UUID.fromString("ef2d1c0b-7ddb-446d-80d4-77301cbd4ffa")
  private val role = Role.Predef.Developer

  private val repository = mock[RoleRepository]
  private val userTeamsClient = mock[UserTeamsClient]
  private val service = new RoleServiceImpl(repository, userTeamsClient)

  "RoleServiceImpl" when {

    "create" should {
      "normalize valid role names spaces" in {

        val roleName = " Role   Name"
        val normalizedRoleName = "Role Name"
        val normalizedRole = Role(
          id = UUID.fromString("79446410-73ad-4122-8110-0904974c2738"),
          name = normalizedRoleName
        )

        repository.insert _ expects normalizedRoleName returns EitherT.pure(normalizedRole)

        service.create(roleName)
          .value
          .asserting(_.value shouldBe normalizedRole)
      }

      "invalidate blank name" in {
        val roleName = "   "
        service.create(roleName)
          .value
          .asserting(_.left.value shouldBe a[ValidationError])
      }

      "invalidate names longer that 100 characters" in {
        val roleName = repeat("x", 101)
        service.create(roleName)
          .value
          .asserting(_.left.value shouldBe a[ValidationError])
      }

      "invalidate non-letter characters" in {
        val roleName = "Role N4me"
        service.create(roleName)
          .value
          .asserting(_.left.value shouldBe a[ValidationError])
      }
    }

    "assign" should {

      "return error given repository error" in {

        val team = Team(
          id = teamId,
          name = "Error",
          teamLeadId = UUID.fromString("af4c850f-b911-4ad2-a692-93600d6e8461"),
          teamMemberIds = Set(
            UUID.fromString("abbeca3b-5ad6-4a55-8652-6f599a93d632"),
            UUID.fromString("f74a61e9-a618-4c43-9b31-7de524423186"),
            userId
          )
        )

        val error = FieldError("*", "test")

        repository.upsertMembershipRole _ expects (teamId, userId, role.id) returns EitherT.leftT(error)
        userTeamsClient.findTeamById _ expects teamId returns OptionT.some(team)

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_ shouldBe Some(Left(error)))

      }

      "return none given nonexistent team" in {

        userTeamsClient.findTeamById _ expects teamId returns OptionT.none

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_ shouldBe None)

      }

      "return none given user is not a team member or lead" in {

        val team = Team(
          id = teamId,
          name = "Not a team member or lead",
          teamLeadId = UUID.fromString("68375515-cd6f-4fd7-963e-277de152f9c1"),
          teamMemberIds = Set(
            UUID.fromString("9853d919-743e-46f8-bd5a-e234cf103297"),
            UUID.fromString("6960b480-d5da-46f3-a00f-9ccc72af3ee6")
          )
        )

        userTeamsClient.findTeamById _ expects teamId returns OptionT.some(team)

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_ shouldBe None)

      }

      "return true given user is team lead" in {

        val team = Team(
          id = teamId,
          name = "User is team lead",
          teamLeadId = userId,
          teamMemberIds = Set(
            UUID.fromString("4e52b2a4-bb8a-46df-b62d-faa94607129e"),
            UUID.fromString("6d772951-d4d3-4132-8798-0df50364f790")
          )
        )

        repository.upsertMembershipRole _ expects (teamId, userId, role.id) returns EitherT.rightT(())
        userTeamsClient.findTeamById _ expects teamId returns OptionT.some(team)

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_ shouldBe Some(Right(())))

      }

      "return true given user is a team member" in {

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

        repository.upsertMembershipRole _ expects (teamId, userId, role.id) returns EitherT.rightT(())
        userTeamsClient.findTeamById _ expects teamId returns OptionT.some(team)

        service.assign(teamId, userId, role.id)
          .value
          .asserting(_ shouldBe Some(Right(())))

      }

    }

    "roleLookup" should {
      "return none given nonexistent team" in {

        repository.findByMembership _ expects (teamId, userId) returns OptionT.none
        userTeamsClient.findTeamById _ expects teamId returns OptionT.none

        service.roleLookup(teamId, userId)
          .value
          .asserting(_ shouldBe None)

      }

      "return none given user is not a team member" in {

        val team = Team(
          id = teamId,
          name = "User is not a team member",
          teamLeadId = UUID.fromString("192054bd-436f-48d9-86ba-4aa86fcdb7de"),
          teamMemberIds = Set(
            UUID.fromString("3f229bb4-eec1-4c95-93e5-79c902eb7a40"),
            UUID.fromString("667d5d04-e216-41fa-85c0-51a187ec0a45")
          )
        )

        repository.findByMembership _ expects (teamId, userId) returns OptionT.none
        userTeamsClient.findTeamById _ expects teamId returns OptionT.some(team)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_ shouldBe None)

      }

      "return Developer role given team member has not been explicitly assigned a role" in {

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

        repository.findByMembership _ expects (teamId, userId) returns OptionT.none
        userTeamsClient.findTeamById _ expects teamId returns OptionT.some(team)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_ shouldBe Some(Role.Predef.Developer))

      }

      "return assigned role given team member has a role assigned" in {

        val assignedRole = Role(
          id = UUID.fromString("21eaaefe-25c3-4ca0-b45c-38b559033876"),
          name = "Assigned Role"
        )

        repository.findByMembership _ expects (teamId, userId) returns OptionT.pure(assignedRole)

        service.roleLookup(teamId, userId)
          .value
          .asserting(_ shouldBe Some(assignedRole))

      }

    }
  }
}

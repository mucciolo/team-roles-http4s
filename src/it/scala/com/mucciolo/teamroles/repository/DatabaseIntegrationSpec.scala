package com.mucciolo.teamroles.repository

import cats.implicits._
import com.mucciolo.teamroles.domain._
import com.mucciolo.teamroles.util.Database.DataSourceTransactor
import doobie.implicits._
import doobie.postgres.implicits._

import java.util.UUID

final class DatabaseIntegrationSpec extends PostgresIntegrationSpec {

  private lazy val repository: RoleRepository = new PostgresRoleRepository(transactor)

  override protected def clearDatabase(transactor: DataSourceTransactor): Unit = {
    val clearRoleAssignments = sql"DELETE FROM team_member_role".update.run
    val clearRoles =
      sql"""
         DELETE FROM roles
         WHERE id NOT IN (
           ${Role.Predef.Developer.id}, ${Role.Predef.Tester.id}, ${Role.Predef.ProductOwner.id}
         )
         """.update.run

    (clearRoleAssignments, clearRoles).mapN(_ + _).transact(transactor).void.unsafeRunSync()
  }

  "Database" when {
    "migrated" should {
      "have developer role predefined" in {
        repository
          .findById(Role.Predef.Developer.id)
          .value
          .asserting(_.value shouldBe Role.Predef.Developer)
      }

      "have product owner role predefined" in {
        repository
          .findById(Role.Predef.ProductOwner.id)
          .value
          .asserting(_.value shouldBe Role.Predef.ProductOwner)
      }

      "have tester role predefined" in {
        repository
          .findById(Role.Predef.Tester.id)
          .value
          .asserting(_.value shouldBe Role.Predef.Tester)
      }
    }

  }

  "PostgresRoleRepository" when {
    "insert" should {
      "return inserted role given a unique name" in {
        for {
          errorXorRole <- repository.insert("Unique Name").value
          insertedRole = errorXorRole.value
          maybeFetchedRole <- repository.findById(insertedRole.id).value
          fetchedRole = maybeFetchedRole.value
        } yield insertedRole shouldBe fetchedRole
      }

      "return error given a existing name" in {
        val existingName = "Duplicate Name"

        (repository.insert(existingName) *> repository.insert(existingName))
          .value
          .asserting(_ shouldBe Left(FieldError("role.name", "already.exists")))
      }
    }

    "upsertMembershipRole" should {

      "assign the role to membership" in {

        val teamId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        for {
          errXorRole <- repository.insert("Role").value
          role = errXorRole.value
          _ <- repository.upsertMembershipRole(teamId, userId, role.id).value
          insertedRole <- repository.findByMembership(teamId, userId).value
        } yield {
          insertedRole.value shouldBe role
        }
      }

      "update the role assigned to membership" in {

        val teamId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        for {
          errXorFirstRole <- repository.insert("First").value
          firstRole = errXorFirstRole.value
          _ <- repository.upsertMembershipRole(teamId, userId, firstRole.id).value
          errXorSecondRole <- repository.insert("Second").value
          secondRole = errXorSecondRole.value
          _ <- repository.upsertMembershipRole(teamId, userId, secondRole.id).value
          updatedRole <- repository.findByMembership(teamId, userId).value
        } yield {
          updatedRole.value shouldBe secondRole
        }
      }

      "return error if the role does not exist" in {

        val teamId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val roleId = UUID.randomUUID()

        repository
          .upsertMembershipRole(teamId, userId, roleId)
          .value
          .asserting(_ shouldBe Left(FieldError("role.id", "not.found")))
      }

    }

    "findMemberships" should {

      "return a empty list when there is no membership associated to role" in {
        for {
          errXorRole <- repository.insert("Role").value
          role = errXorRole.value
          memberships <- repository.findMemberships(role.id)
        } yield memberships shouldBe List.empty
      }

      "return a list of memberships given existing role" in {

        val membership1 = Membership(
          teamId = UUID.randomUUID(),
          userId = UUID.randomUUID()
        )
        val membership2 = Membership(
          teamId = UUID.randomUUID(),
          userId = UUID.randomUUID()
        )
        val expectedMemberships = List(membership1, membership2)

        for {
          errXorRole <- repository.insert("Role").value
          role = errXorRole.value
          _ <- repository.upsertMembershipRole(membership1.teamId, membership1.userId, role.id).value
          _ <- repository.upsertMembershipRole(membership2.teamId, membership2.userId, role.id).value
          memberships <- repository.findMemberships(role.id)
        } yield memberships shouldBe expectedMemberships
      }
    }
  }

}

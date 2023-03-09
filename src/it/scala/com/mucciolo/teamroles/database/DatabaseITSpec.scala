package com.mucciolo.teamroles.database

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import cats.implicits._
import com.dimafeng.testcontainers.DockerComposeContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.mucciolo.teamroles.domain._
import com.mucciolo.teamroles.repository.SQLRoleRepository
import com.mucciolo.teamroles.util.Database
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.FixtureAsyncWordSpec
import org.scalatest.{EitherValues, OptionValues}

import java.util.UUID

class DatabaseITSpec extends FixtureAsyncWordSpec with Matchers with EitherValues
  with OptionValues with AsyncIOSpec with CatsResourceIO[SQLRoleRepository]
  with TestContainerForAll {

  override val containerDef: DockerComposeContainer.Def = PostgresContainer.Def

  override val resource: Resource[IO, SQLRoleRepository] = for {
    transactor <- Database.newTransactor(PostgresContainer.Conf)
    _ <- Database.migrate(transactor)
  } yield new SQLRoleRepository(transactor)


  "Database" when {
    "migrated" should {
      "have developer role predefined" in { repository =>
        repository
          .findById(Role.Predef.developer.id)
          .value
          .asserting(_.value shouldBe Role.Predef.developer)
      }

      "have product owner role predefined" in { repository =>
        repository
          .findById(Role.Predef.productOwner.id)
          .value
          .asserting(_.value shouldBe Role.Predef.productOwner)
      }

      "have tester role predefined" in { repository =>
        repository
          .findById(Role.Predef.tester.id)
          .value
          .asserting(_.value shouldBe Role.Predef.tester)
      }
    }

  }

  "SQLRoleRepository" when {

    "insert" should {
      "return inserted role given a unique name" in { repository =>
        for {
          errorXorRole <- repository.insert("New Role").value
          insertedRole = errorXorRole.value
          maybeInsertedRoleByName <- repository.findById(insertedRole.id).value
          insertedRoleByName = maybeInsertedRoleByName.value
        } yield insertedRole shouldBe insertedRoleByName
      }

      "should return error given a existing name" in { repository =>
        val existingName = Role.Predef.developer.name

        repository
          .insert(existingName)
          .value
          .asserting(_ shouldBe Left(FieldError("role.name", "already.exists")))
      }
    }

    "upsertMembershipRole" should {

      "return true given the role exists and there is no membership" in { repository =>

        val teamId = UUID.fromString("93f3831b-c2fb-4344-a381-d83da651befc")
        val userId = UUID.fromString("4992e273-4f42-4bb6-a2b8-0f36ad4e04bc")
        val expectedRole = Role.Predef.developer
        val roleId = expectedRole.id

        for {
          errorXorInserted <- repository.upsertMembershipRole(teamId, userId, roleId).value
          hasAnyRowInserted = errorXorInserted.value
          insertedRole <- repository.findByMembership(teamId, userId).value
        } yield {
          hasAnyRowInserted shouldBe true
          insertedRole.value shouldBe expectedRole
        }
      }

      "return true given the role exists and there is already a membership" in { repository =>

        val teamId = UUID.fromString("b379e724-ec7f-46ae-8989-0eedabd37430")
        val userId = UUID.fromString("178d8120-9199-46ab-a61a-868668a30e23")
        val expectedRole = Role.Predef.tester
        val roleId = expectedRole.id

        for {
          errorXorInserted <- repository.upsertMembershipRole(teamId, userId, Role.Predef.developer.id).value
          hasAnyRowInserted = errorXorInserted.value
          errorXorUpdated <- repository.upsertMembershipRole(teamId, userId, roleId).value
          hasAnyRowUpdated = errorXorUpdated.value
          updatedRole <- repository.findByMembership(teamId, userId).value
        } yield {
          hasAnyRowInserted shouldBe true
          hasAnyRowUpdated shouldBe true
          updatedRole.value shouldBe expectedRole
        }
      }

      "return error if the role does not exist" in { repository =>

        val teamId = UUID.fromString("d3afd082-f0b6-48a2-a7d7-00298d363c77")
        val userId = UUID.fromString("252be669-8314-41bc-9d52-95863300c5c6")
        val roleId = UUID.fromString("6580a126-1fe8-4e3c-9d9b-3417902bcadc")

        repository
          .upsertMembershipRole(teamId, userId, roleId)
          .value
          .asserting(_ shouldBe Left(FieldError("role.id", "not.found")))
      }

    }

    "findMemberships" should {

      "return a list of memberships given existing role" in { repository =>

        val membership1 = Membership(
          teamId = UUID.fromString("5f6c189b-a76e-486c-b847-c8a05ed4dfd2"),
          userId = UUID.fromString("85b2595a-045f-4a10-95e3-3ba633cff2c1")
        )
        val membership2 = Membership(
          teamId = UUID.fromString("c71226b2-1a3e-4c1c-9461-b1363f2a10ed"),
          userId = UUID.fromString("e54cb315-10ad-44ee-9b66-c47a6bb2f3b1")
        )
        val expectedMemberships = List(membership1, membership2)
        val role = Role.Predef.productOwner

        repository.upsertMembershipRole(membership1.teamId, membership1.userId, role.id).value *>
          repository.upsertMembershipRole(membership2.teamId, membership2.userId, role.id).value *>
          repository.findMemberships(role.id).asserting(_ shouldBe expectedMemberships)

      }

    }

  }
}

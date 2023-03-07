package com.mucciolo.teamroles.database

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import cats.implicits._
import com.dimafeng.testcontainers.DockerComposeContainer.ComposeFile
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import com.mucciolo.teamroles.config.DatabaseConf
import com.mucciolo.teamroles.repository.{PredefRoles, SQLRoleRepository}
import org.scalatest.freespec.FixtureAsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.testcontainers.containers.wait.strategy.Wait
import com.mucciolo.teamroles.core.Domain._
import com.mucciolo.teamroles.util.Database

import java.io.File
import java.util.UUID
import scala.concurrent.ExecutionContext

class DatabaseIntegrationSpec extends FixtureAsyncFreeSpec with Matchers with EitherValues
  with OptionValues with AsyncIOSpec with CatsResourceIO[SQLRoleRepository]
  with TestContainerForAll {

  override val containerDef: DockerComposeContainer.Def =
    DockerComposeContainer.Def(
      ComposeFile(Left(new File("src/it/resources/docker-compose.yml"))),
      tailChildContainers = true,
      exposedServices = Seq(
        ExposedService(
          "postgres",
          5433,
          Wait.forLogMessage(".*database system is ready to accept connections.*", 2)
        )
      )
    )

  private val databaseConf = DatabaseConf(
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5433/test",
    user = "test",
    pass = "test"
  )

  override val resource: Resource[IO, SQLRoleRepository] = for {
    transactor <- Database.newTransactor(databaseConf, ExecutionContext.global)
    _ <- Database.migrate(transactor)
  } yield new SQLRoleRepository(transactor)


  "Database" - {
    "should have developer role predefined" in { repository =>
      repository
        .findById(PredefRoles.developer.id)
        .value
        .asserting(_.value shouldBe PredefRoles.developer)
    }

    "should have product owner role predefined" in { repository =>
      repository
        .findById(PredefRoles.productOwner.id)
        .value
        .asserting(_.value shouldBe PredefRoles.productOwner)
    }

    "should have tester role predefined" in { repository =>
      repository
        .findById(PredefRoles.tester.id)
        .value
        .asserting(_.value shouldBe PredefRoles.tester)
    }
  }

  "SQLRoleRepository" - {

    "insert" - {
      "should return inserted role given a unique name" in { repository =>
        for {
          errorXorRole <- repository.insert("New Role").value
          insertedRole = errorXorRole.value
          maybeInsertedRoleByName <- repository.findById(insertedRole.id).value
          insertedRoleByName = maybeInsertedRoleByName.value
        } yield insertedRole shouldBe insertedRoleByName
      }

      "should return error given a existing name" in { repository =>
        val existingName = PredefRoles.developer.name

        repository
          .insert(existingName)
          .value
          .asserting(_.left.value shouldBe "Role name already exists")
      }
    }

    "upsertMembershipRole" - {

      "should return true given the role exists and there is no membership" in { repository =>

        val teamId = UUID.fromString("93f3831b-c2fb-4344-a381-d83da651befc")
        val userId = UUID.fromString("4992e273-4f42-4bb6-a2b8-0f36ad4e04bc")
        val expectedRole = PredefRoles.developer
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

      "should return true given the role exists and there is already a membership" in { repository =>

        val teamId = UUID.fromString("b379e724-ec7f-46ae-8989-0eedabd37430")
        val userId = UUID.fromString("178d8120-9199-46ab-a61a-868668a30e23")
        val expectedRole = PredefRoles.tester
        val roleId = expectedRole.id

        for {
          errorXorInserted <- repository.upsertMembershipRole(teamId, userId, PredefRoles.developer.id).value
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

      "should return error if the role does not exist" in { repository =>

        val teamId = UUID.fromString("d3afd082-f0b6-48a2-a7d7-00298d363c77")
        val userId = UUID.fromString("252be669-8314-41bc-9d52-95863300c5c6")
        val roleId = UUID.fromString("6580a126-1fe8-4e3c-9d9b-3417902bcadc")

        repository
          .upsertMembershipRole(teamId, userId, roleId)
          .value
          .map(_.left.value shouldBe "Role not found")
      }

    }

    "findMemberships" - {

      "should return a list of memberships given existing role" in { repository =>

        val membership1 = Membership(
          teamId = UUID.fromString("5f6c189b-a76e-486c-b847-c8a05ed4dfd2"),
          userId = UUID.fromString("85b2595a-045f-4a10-95e3-3ba633cff2c1")
        )
        val membership2 = Membership(
          teamId = UUID.fromString("c71226b2-1a3e-4c1c-9461-b1363f2a10ed"),
          userId = UUID.fromString("e54cb315-10ad-44ee-9b66-c47a6bb2f3b1")
        )
        val expectedMemberships = List(membership1, membership2)
        val role = PredefRoles.productOwner

        repository.upsertMembershipRole(membership1.teamId, membership1.userId, role.id).value *>
          repository.upsertMembershipRole(membership2.teamId, membership2.userId, role.id).value *>
          repository.findMemberships(role.id).asserting(_ shouldBe expectedMemberships)

      }

    }

  }
}

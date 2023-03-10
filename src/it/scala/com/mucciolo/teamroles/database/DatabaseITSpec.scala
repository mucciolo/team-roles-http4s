package com.mucciolo.teamroles.database

import cats.effect.testing.scalatest.{AssertingSyntax, AsyncIOSpec}
import cats.implicits._
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.mucciolo.teamroles.domain._
import com.mucciolo.teamroles.repository.{RoleRepository, SQLRoleRepository}
import com.mucciolo.teamroles.util.Database
import com.mucciolo.teamroles.util.Database.DataSourceTransactor
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.implicits._
import doobie.postgres.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterEach, EitherValues, OptionValues}
import org.testcontainers.utility.DockerImageName

import java.util.UUID

class DatabaseITSpec extends AsyncWordSpec with Matchers with EitherValues with AsyncIOSpec
  with OptionValues with AssertingSyntax with TestContainerForAll with BeforeAndAfterEach {

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    DockerImageName.parse("postgres:15.2-alpine")
  )

  private var transactor: DataSourceTransactor = _
  private var repository: RoleRepository = _

  override def afterContainersStart(container: PostgreSQLContainer): Unit = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(container.jdbcUrl)
    hikariConfig.setUsername(container.username)
    hikariConfig.setPassword(container.password)

    val dataSource = new HikariDataSource(hikariConfig)
    transactor = Database.newTransactor(dataSource)
    repository = new SQLRoleRepository(transactor)

    Database.migrate(dataSource)
  }

  override protected def beforeEach(): Unit = {
    val clearRoleAssignments = sql"DELETE FROM team_member_role".update.run
    val clearRoles =
      sql"""
         DELETE FROM roles
         WHERE id NOT IN (
           ${Role.Predef.Developer.id}, ${Role.Predef.Tester.id}, ${Role.Predef.ProductOwner.id}
         )
         """.update.run

    (clearRoleAssignments, clearRoles).mapN(_ + _).transact(transactor).unsafeRunSync()
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

  "SQLRoleRepository" when {
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

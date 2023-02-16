package com.mucciolo.service

import cats.data.EitherT
import cats.effect.{IO, Ref}
import cats.implicits._
import com.mucciolo.client.userteams.{Team, UserTeamsClient}
import com.mucciolo.repository.{Role, RoleRepository}
import org.apache.commons.lang3.StringUtils._

import java.util.UUID

final class RoleServiceImpl(
  repository: RoleRepository, userTeamsClient: UserTeamsClient
) extends RoleService {

  private val DeveloperRole: IO[Ref[IO, Role]] = Ref.ofEffect(
    repository.findByName("Developer")
      .getOrRaise(new NoSuchElementException("Missing required Developer role"))
  )

  private def normalizeRoleName(roleName: String): String = {
    normalizeSpace(roleName)
  }

  override def create(roleName: String): EitherT[IO, String, Role] = {
    val normalizedRoleName = normalizeRoleName(roleName)
    repository.insert(normalizedRoleName)
  }

  private def isUserTeamMember(userId: UUID, teamId: UUID): EitherT[IO, String, Boolean] =
    findTeamById(teamId)
      .map(team => team.teamLeadId == userId || team.teamMemberIds.contains(userId))

  private def findTeamById(teamId: UUID): EitherT[IO, String, Team] = {
    EitherT.fromOptionF(userTeamsClient.findTeamById(teamId), "Team not found")
  }

  override def assign(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, String, Boolean] = {
    isUserTeamMember(userId, teamId).ifM(
      repository.upsertMembershipRole(teamId, userId, roleId),
      EitherT.leftT("User is not a team member")
    )
  }

  private def defaultRole: IO[Role] = DeveloperRole.flatMap(_.get)

  override def roleLookup(teamId: UUID, userId: UUID): EitherT[IO, String, Role] = {
    isUserTeamMember(userId, teamId).ifM(
      EitherT.right(repository.findByMembership(teamId, userId).getOrElseF(defaultRole)),
      EitherT.leftT("User is not a team member")
    )
  }
}

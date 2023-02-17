package com.mucciolo.service

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.data._
import cats.implicits._
import com.mucciolo.client.userteams.{Team, UserTeamsClient}
import com.mucciolo.repository.{PredefRoles, Role, RoleRepository}
import org.apache.commons.lang3.StringUtils._

import java.util.UUID

final class RoleServiceImpl(
  repository: RoleRepository, userTeamsClient: UserTeamsClient
) extends RoleService {

  private val DefaultRole: Role = PredefRoles.developer

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

  override def assign(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, String, Option[Boolean]] = {

    def upsertMembershipRole = isUserTeamMember(userId, teamId).ifM(
      repository.upsertMembershipRole(teamId, userId, roleId).map(Some(_)),
      EitherT.leftT("User is not a team member")
    ).value

    EitherT(isRoleDefined(roleId).ifM(upsertMembershipRole, IO(Right(None))))

  }

  override def roleLookup(teamId: UUID, userId: UUID): EitherT[IO, String, Role] = {
    isUserTeamMember(userId, teamId).ifM(
      EitherT.right(repository.findByMembership(teamId, userId).getOrElse(DefaultRole)),
      EitherT.leftT("User is not a team member")
    )
  }

  private def isRoleDefined(roleId: UUID): IO[Boolean] = repository.findById(roleId).isDefined

  override def membershipLookup(roleId: UUID): OptionT[IO, List[Membership]] = {
    OptionT(
      isRoleDefined(roleId).flatMap { isRoleDefined =>
        Option.when(isRoleDefined)(repository.findMemberships(roleId)).sequence
      }
    )
  }

}

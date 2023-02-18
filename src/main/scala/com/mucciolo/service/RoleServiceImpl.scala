package com.mucciolo.service

import cats.data.{EitherT, OptionT}
import cats.effect.IO
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
    userTeamsClient.findTeamById(teamId).toRight("Team not found")
  }

  override def assign(teamId: UUID, userId: UUID, roleId: UUID): EitherT[IO, String, Option[Boolean]] = {
    isUserTeamMember(userId, teamId).ifM(
      repository.upsertMembershipRole(teamId, userId, roleId).map(Some(_)),
      EitherT.leftT("User is not a team member")
    )
  }

  override def roleLookup(teamId: UUID, userId: UUID): EitherT[IO, String, Role] = {
    EitherT(
      repository
        .findByMembership(teamId, userId)
        .value
        .flatMap {
          case Some(role) => IO(Right(role))
          case None =>
            isUserTeamMember(userId, teamId)
              .subflatMap(x => Either.cond(x, DefaultRole, "User is not a team member"))
              .value
        }
    )
  }

  private def isRoleDefined(roleId: UUID): IO[Boolean] = repository.findById(roleId).isDefined

  override def membershipLookup(roleId: UUID): OptionT[IO, List[Membership]] = {
    OptionT(
      isRoleDefined(roleId)
        .flatMap(Option.when(_)(repository.findMemberships(roleId)).sequence)
    )
  }

}

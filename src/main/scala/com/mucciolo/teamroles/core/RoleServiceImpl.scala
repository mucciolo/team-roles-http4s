package com.mucciolo.teamroles.core

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.implicits._
import com.mucciolo.teamroles.domain.Validators.roleNameValidator
import com.mucciolo.teamroles.domain._
import com.mucciolo.teamroles.repository.RoleRepository
import com.mucciolo.teamroles.userteams.UserTeamsClient
import org.apache.commons.lang3.StringUtils._

import java.util.UUID

final class RoleServiceImpl(
  repository: RoleRepository, userTeamsClient: UserTeamsClient
) extends RoleService {

  private val DefaultRole: Role = Role.Predef.developer

  private def normalizeRoleName(roleName: RoleName): RoleName = {
    normalizeSpace(roleName)
  }

  override def create(roleName: RoleName): EitherT[IO, Error, Role] = {
    EitherT.fromEither[IO](roleNameValidator.validate(roleName))
      .map(normalizeRoleName)
      .flatMap(repository.insert)
  }

  private def isUserTeamMember(userId: UUID, teamId: UUID): OptionT[IO, Boolean] =
    userTeamsClient
      .findTeamById(teamId)
      .map(team => team.teamLeadId == userId || team.teamMemberIds.contains(userId))

  override def assign(teamId: UUID, userId: UUID, roleId: UUID): OptionT[IO, Either[Error, Boolean]] = {
    isUserTeamMember(userId, teamId)
      .filter(_ == true)
      .semiflatMap(_ => repository.upsertMembershipRole(teamId, userId, roleId).value)
  }

  override def roleLookup(teamId: UUID, userId: UUID): OptionT[IO, Role] = {
    repository
      .findByMembership(teamId, userId)
      .orElse(isUserTeamMember(userId, teamId).filter(_ == true).map(_ => DefaultRole))
  }

  private def isRoleDefined(roleId: UUID): IO[Boolean] = repository.findById(roleId).isDefined

  override def membershipLookup(roleId: UUID): OptionT[IO, List[Membership]] = {
    OptionT(
      isRoleDefined(roleId)
        .flatMap(Option.when(_)(repository.findMemberships(roleId)).sequence)
    )
  }

}

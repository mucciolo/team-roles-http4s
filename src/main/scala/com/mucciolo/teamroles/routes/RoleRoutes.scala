package com.mucciolo.teamroles.routes

import cats.effect.IO
import cats.implicits._
import com.mucciolo.teamroles.core.RoleService
import com.mucciolo.teamroles.util.Validator
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object RoleRoutes extends Http4sDsl[IO] {

  def apply(
    roleService: RoleService,
    roleValidator: Validator[RoleCreationRequest] = RoleCreationRequest.validator,
    assignmentValidator: Validator[RoleAssignmentRequest] = RoleAssignmentRequest.validator,
  ): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ POST -> Root / "teams" / "roles" =>
      req.decodeJson[RoleCreationRequest].flatMap { roleCreationRequest =>
        roleValidator.validate(roleCreationRequest) match {

          case Left(errors) =>
            BadRequest(errors)

          case Right(roleCreationRequest) =>
            roleService
              .create(roleCreationRequest.name.get)
              .foldF(err => BadRequest(err), role => Created(role))
        }
      }

    case req @ PUT -> Root / "teams" / UUIDVar(teamId) / "members" / UUIDVar(userId) / "role" =>
      req.decodeJson[RoleAssignmentRequest].flatMap { roleAssignmentRequest =>
        assignmentValidator.validate(roleAssignmentRequest) match {

          case Left(errors) =>
            BadRequest(errors)

          case Right(roleCreationRequest) =>
            roleService
              .assign(teamId, userId, roleCreationRequest.roleId.get)
              .foldF(NotFound())(_.fold(err => BadRequest(err), _ => NoContent()))
        }
      }

    case GET -> Root / "teams" / UUIDVar(teamId) / "members" / UUIDVar(userId) / "role" =>
      roleService
        .roleLookup(teamId, userId)
        .foldF(NotFound())(Ok(_))

    case GET -> Root / "roles" / UUIDVar(roleId) / "assignments" =>
      roleService
        .membershipLookup(roleId)
        .foldF(NotFound())(Ok(_))

  }
}

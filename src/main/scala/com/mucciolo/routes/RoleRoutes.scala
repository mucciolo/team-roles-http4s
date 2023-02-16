package com.mucciolo.routes

import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import cats.implicits._
import com.mucciolo.service.RoleService
import com.mucciolo.util.Validator
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
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

          case Invalid(errors) =>
            BadRequest(Error(errors))

          case Valid(roleCreationRequest) =>
            roleService
              .create(roleCreationRequest.name.get)
              .foldF(err => BadRequest(Error(err)), role => Created(role))
        }
      }

    case req @ PUT -> Root / "teams" / UUIDVar(teamId) / "members" / UUIDVar(userId) / "role" =>
      req.decodeJson[RoleAssignmentRequest].flatMap { roleAssignmentRequest =>
        assignmentValidator.validate(roleAssignmentRequest) match {

          case Invalid(errors) =>
            BadRequest(Error(errors))

          case Valid(roleCreationRequest) =>
            roleService
              .assign(teamId, userId, roleCreationRequest.roleId.get)
              .foldF(err => BadRequest(Error(err)), _ => NoContent())
        }
      }

    case GET -> Root / "teams" / UUIDVar(teamId) / "members" / UUIDVar(userId) / "role" =>
      roleService
        .roleLookup(teamId, userId)
        .foldF(err => BadRequest(Error(err)), Ok(_))

  }
}

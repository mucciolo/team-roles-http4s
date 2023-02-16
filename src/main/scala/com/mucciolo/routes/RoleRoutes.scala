package com.mucciolo.routes

import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import cats.implicits._
import com.mucciolo.repository.RoleInsert
import com.mucciolo.service.RoleService
import com.mucciolo.util.Validator
import io.circe.generic.encoding.DerivedAsObjectEncoder.deriveEncoder
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object RoleRoutes extends Http4sDsl[IO] {

  private object QueryParam {
    object Limit extends QueryParamDecoderMatcher[Int]("limit")
    object Offset extends QueryParamDecoderMatcher[Int]("offset")
    object MinValue extends OptionalQueryParamDecoderMatcher[Int]("min")
  }

  def apply(
    roleService: RoleService,
    roleValidator: Validator[RoleInsert] = RoleInsert.validator
  ): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ POST -> Root / "teams" / "roles" =>
      req.decodeJson[RoleInsert].flatMap { role =>
        roleValidator.validate(role) match {

          case Invalid(errors) =>
            BadRequest(Error(errors))

          case Valid(newRole) =>
            roleService
              .create(newRole)
              .foldF(err => BadRequest(Error(err)), role => Created(role))
        }
      }

    //    case GET -> DataPath :? QueryParam.Limit(limit) +& QueryParam.Offset(offset) +& QueryParam.MinValue(min)  =>
    //      Ok(
    //        Stream("[") ++ roleRepository.get(limit, offset, min).map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"),
    //        `Content-Type`(MediaType.application.json)
    //      )
    //
    //    case GET -> DataPath / LongVar(id) =>
    //      for {
    //        result <- roleRepository.findById(id)
    //        response <- result match {
    //          case Some(data) => Ok(data.asJson)
    //          case None => NotFound()
    //        }
    //      } yield response
    //
    //    case req @ PUT -> DataPath / LongVar(id) =>
    //      for {
    //        data <- req.decodeJson[Role]
    //        result <- roleRepository.update(id, data)
    //        response <- result match {
    //          case Some(data) => Ok(data.asJson)
    //          case None => NotFound()
    //        }
    //      } yield response

  }
}

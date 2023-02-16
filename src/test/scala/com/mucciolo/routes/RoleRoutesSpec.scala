package com.mucciolo.routes

import cats.data.Validated._
import cats.data._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxEitherId
import com.mucciolo.repository._
import com.mucciolo.service.RoleService
import com.mucciolo.util.Validator
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Request, Response, Status}
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class RoleRoutesSpec extends AnyFreeSpec with MockFactory with Matchers {

  private val service = stub[RoleService]

  private def send(routes: HttpRoutes[IO])(request: Request[IO]): Response[IO] = {
    routes.orNotFound(request).unsafeRunSync()
  }

  "Role creation" - {

    "should return 201 with newly created object given valid role" in {

      val routes = RoleRoutes(service)
      val roleToBeCreated = RoleInsert("New Role")
      val expectedCreatedRole = Role(
        id = UUID.fromString("b93558f5-0ac8-47f8-8cf0-7b131364d464"),
        name = roleToBeCreated.name
      )
      val serviceCreateReturn = EitherT(IO.pure(expectedCreatedRole.asRight[String]))

      (service.create _).when(roleToBeCreated).returns(serviceCreateReturn)

      val request = Request[IO](POST, uri"/teams/roles").withEntity(roleToBeCreated)
      val response = send(routes)(request)

      response.status shouldBe Status.Created
      response.as[Role].unsafeRunSync() shouldBe expectedCreatedRole

    }

    "should return 400 with a list of errors given invalid role" in {

      val invalidRole = RoleInsert("Invalid")
      val roleValidator: Validator[RoleInsert] =
        role => condNec(role.name != invalidRole.name, role, "Invalid role name")
      val routes = RoleRoutes(service, roleValidator)

      val request = Request[IO](POST, uri"/teams/roles").withEntity(invalidRole)
      val response = send(routes)(request)
      val expectedError = Error("Invalid role name")

      response.status shouldBe Status.BadRequest
      response.as[Error].unsafeRunSync() shouldBe expectedError

    }

  }

//    "update data when id exists" in {
//
//      val id = 1
//      val requestData = Data(None, 2)
//      val expectedCreatedData = requestData.copy(id = Some(id))
//
//      (repository.update _).when(id, requestData).returns(IO.pure(Some(expectedCreatedData)))
//
//      val requestJson = requestData.asJson.toString()
//      val request = Request[IO](PUT, Uri.unsafeFromString(s"/data/$id")).withEntity(requestJson)
//      val response = send(request)
//
//      response.status shouldBe Status.Ok
//      response.as[Json].unsafeRunSync() shouldBe expectedCreatedData.asJson
//
//    }
//
//    "do not update data when id does not exist" in {
//
//      val id = 1
//      val requestData = Data(None, 2)
//
//      (repository.update _).when(id, requestData).returns(IO.pure(None))
//
//      val requestJson = requestData.asJson.toString()
//      val request = Request[IO](PUT, Uri.unsafeFromString(s"/data/$id")).withEntity(requestJson)
//      val response = send(request)
//
//      response.status shouldBe Status.NotFound
//
//    }
//
//    "return a single data when id exists" in {
//
//      val id = 1
//      val requestData = Data(None, 2)
//      val expectedCreatedData = requestData.copy(id = Some(id))
//
//      (repository.findById _).when(id).returns(IO.pure(Some(expectedCreatedData)))
//
//      val requestJson = requestData.asJson.toString()
//      val request = Request[IO](GET, Uri.unsafeFromString(s"/data/$id")).withEntity(requestJson)
//      val response = send(request)
//
//      response.status shouldBe Status.Ok
//      response.as[Json].unsafeRunSync() shouldBe expectedCreatedData.asJson
//
//    }
//
//    "do not return a single data when id does not exist" in {
//
//      val id = 1
//      val requestData = Data(None, 2)
//
//      (repository.findById _).when(id).returns(IO.pure(None))
//
//      val requestJson = requestData.asJson.toString()
//      val request = Request[IO](GET, Uri.unsafeFromString(s"/data/$id")).withEntity(requestJson)
//      val response = send(request)
//
//      response.status shouldBe Status.NotFound
//
//    }
//
//    "return all data" in {
//
//      val dataStream = Stream(
//        Data(Some(1), 11),
//        Data(Some(2), 22),
//        Data(Some(3), 33)
//      )
//      val limit = 10
//      val offset = 0
//
//      (repository.get _).when(limit, offset, None).returns(dataStream)
//
//      val request = Request[IO](GET, Uri.unsafeFromString(s"/data?limit=$limit&offset=$offset"))
//      val response = send(request)
//
//      response.status shouldBe Status.Ok
//      response.as[Json].unsafeRunSync() shouldBe dataStream.toList.asJson
//
//    }
//
//    "delete data when id exists" in {
//
//      val id = 1
//      (repository.delete _).when(id).returns(IO.pure(Some(())))
//
//      val request = Request[IO](DELETE, Uri.unsafeFromString(s"/data/$id"))
//      val response = send(request)
//
//      response.status shouldBe Status.NoContent
//
//    }
//
//    "do not delete data when id does not exist" in {
//
//      val id = 1
//      (repository.delete _).when(id).returns(IO.pure(None))
//
//      val request = Request[IO](DELETE, Uri.unsafeFromString(s"/data/$id"))
//      val response = send(request)
//
//      response.status shouldBe Status.NotFound
//
//    }
//
//  }

}

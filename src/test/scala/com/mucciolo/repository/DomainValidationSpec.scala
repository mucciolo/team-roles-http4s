package com.mucciolo.repository

import cats.data.Validated.Invalid
import com.mucciolo.routes.RoleCreationRequest
import org.apache.commons.lang3.StringUtils.repeat
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class DomainValidationSpec extends AnyFreeSpec with Matchers {

  "RoleInsert validator" - {
    "should invalidate missing name" in {
      val role = RoleCreationRequest(name = None)
      RoleCreationRequest.validator.validate(role) shouldBe a [Invalid[_]]
    }

    "should invalidate blank name" in {
      val role = RoleCreationRequest(name = Some(" "))
      RoleCreationRequest.validator.validate(role) shouldBe a [Invalid[_]]
    }

    "should invalidate names longer that 100 characters" in {
      val role = RoleCreationRequest(name = Some(repeat("c", 101)))
      RoleCreationRequest.validator.validate(role) shouldBe a [Invalid[_]]
    }

    "should invalidate with non-letter characters" in {
      val role = RoleCreationRequest(name = Some("N4m3 #"))
      RoleCreationRequest.validator.validate(role) shouldBe a[Invalid[_]]
    }
  }

}

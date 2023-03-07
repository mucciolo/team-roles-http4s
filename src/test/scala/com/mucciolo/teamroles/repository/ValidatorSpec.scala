package com.mucciolo.teamroles.repository

import com.mucciolo.teamroles.routes.RoleCreationRequest
import org.apache.commons.lang3.StringUtils.repeat
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ValidatorSpec extends AnyWordSpec with Matchers with EitherValues {

  "RoleInsert.validator" when {
    "validate" should {
      "invalidate missing name" in {
        val invalidRole = RoleCreationRequest(name = None)
        RoleCreationRequest.validator.validate(invalidRole).isLeft shouldBe true
      }

      "invalidate blank name" in {
        val invalidRole = RoleCreationRequest(name = Some(" "))
        RoleCreationRequest.validator.validate(invalidRole).isLeft shouldBe true
      }

      "invalidate names longer that 100 characters" in {
        val invalidRole = RoleCreationRequest(name = Some(repeat("c", 101)))
        RoleCreationRequest.validator.validate(invalidRole).isLeft shouldBe true
      }

      "invalidate non-letter characters" in {
        val invalidRole = RoleCreationRequest(name = Some("N4m3 #"))
        RoleCreationRequest.validator.validate(invalidRole).isLeft shouldBe true
      }

      "return valid instance" in {
        val validRole = RoleCreationRequest(name = Some("Valid Role"))
        RoleCreationRequest.validator.validate(validRole).value shouldBe validRole
      }
    }
  }

}

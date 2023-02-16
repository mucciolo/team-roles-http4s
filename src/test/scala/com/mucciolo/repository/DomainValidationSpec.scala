package com.mucciolo.repository

import cats.data.Validated.Invalid
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class DomainValidationSpec extends AnyFreeSpec with Matchers {

  "RoleInsert validator" - {
    "should invalidate null name" in {
      val role = RoleInsert(name = null)
      RoleInsert.validator.validate(role) shouldBe a [Invalid[_]]
    }

    "should invalidate blank name" in {
      val role = RoleInsert(name = " ")
      RoleInsert.validator.validate(role) shouldBe a [Invalid[_]]
    }

    "should invalidate names longer that 100 characters" in {
      val role = RoleInsert(name = "a".repeat(101))
      RoleInsert.validator.validate(role) shouldBe a [Invalid[_]]
    }

    "should invalidate with non-letter characters" in {
      val role = RoleInsert(name = "N4m3 #")
      RoleInsert.validator.validate(role) shouldBe a[Invalid[_]]
    }
  }

}

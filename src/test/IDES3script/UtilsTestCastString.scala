package IDES3script

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import Utils.castString

@RunWith(classOf[JUnitRunner])
class UtilsTestCastString extends AnyFunSuite {
  test("cast string") {
    assert(castString("str") == Right("str"))
  }

  test("cast number") {
    assert(castString(42) == Right("42"))
  }

  test("cast obs") {
    assert(castString(new {
      override def toString: String = "42"
    }) == Left(()))
  }

  test("case Null literal") {
    assert(castString(null) == Left(()))
  }

  test("case Null string") {
    val str: String = null
    assert(castString(str) == Left(()))
  }
}

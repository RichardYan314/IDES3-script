package IDES3script

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import Utils.castSet

@RunWith(classOf[JUnitRunner])
class UtilsTestCastSet extends AnyFunSuite {
  test("cast null") {
    val set = castSet(null)
    assert(set.fold(
      _ => false,
      set => set.isEmpty
    ))
  }

  test("cast singleton") {
    val set = castSet[String]("str")
    assert(set.fold(
      _ => false,
      set => set.size == 1 && set.contains("str")
    ))
  }

  test("cast singleton bad type") {
    val set = castSet[String](new {})
    assert(set.fold(
      _ => true,
      _ => false
    ))
  }

  test("cast set") {
    val hset = new java.util.HashSet[String]
    hset.add("a")
    hset.add("b")
    val set = castSet[String](hset)
    assert(set.fold(
      _ => false,
      set => set.size == 2 && set.contains("a") && set.contains("b")
    ))
  }

  test("cast set runtime type match") {
    val hset = new java.util.HashSet[Any]
    hset.add("a")
    val set = castSet[String](hset)
    assert(set.fold(
      _ => false,
      set => set.size == 1 && set.contains("a")
    ))
  }

  test("cast set runtime type mismatch") {
    val hset = new java.util.HashSet[Any]
    hset.add(1)
    val set = castSet[String](hset)
    assert(set.fold(
      _ => true,
      _ => false
    ))
  }

  test("cast array list") {
    val al = new java.util.ArrayList[String]
    al.add("a")
    al.add("b")
    al.add("a")
    val set = castSet[String](al)
    assert(set.fold(
      _ => false,
      set => set.size == 2 && set.contains("a") && set.contains("b")
    ))
  }
}

package IDES3script

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner

import ExecuteScript.parseSpec
import FSASpec.Spec._
import FSASpec.Spec

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class UtilsTestValidate extends AnyFunSuite {
  val name = "G"
  val layouter = "whatev"

  test("parse empty string") {
    parseSpec("") should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      emptyspec
    ))
  }

  test("parse name only") {
    parseSpec(
      s"""
        | name: $name
        |""".stripMargin) should be
    Success(new FSASpec(
      name,
      layouterDft,
      emptyspec
    ))
  }

  test("parse name null") {
    parseSpec(
      """
        | name:
        |""".stripMargin) should be
    Failure(new IllegalArgumentException(
      "name is not a string"
    ))
  }

  test("parse layouter only") {
    parseSpec(
      s"""
        | layouter: $layouter
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouter,
      emptyspec
    ))
  }

  test("parse layouter null") {
    parseSpec(
      """
        | layouter:
        |""".stripMargin) should be
    Failure(new IllegalArgumentException(
      "layouter is not a string"
    ))
  }

  test("parse spec empty") {
    parseSpec(
      """
        | spec:
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      emptyspec
    ))
  }

  test("parse start state") {
    parseSpec(
      """
        | spec:
        |   start state: 1
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set("1"),
        Set(),
        Set(),
        Set(),
        Set(),
        Map()
      )
    ))
  }

  test("parse start states") {
    parseSpec(
      """
        | spec:
        |   start state: [1, 2]
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set("1", "2"),
        Set(),
        Set(),
        Set(),
        Set(),
        Map()
      )
    ))
  }

  test("parse start states singleton") {
    parseSpec(
      """
        | spec:
        |   start states: 1
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set("1"),
        Set(),
        Set(),
        Set(),
        Set(),
        Map()
      )
    ))
  }

  test("parse table null") {
    parseSpec(
      """
        | spec:
        |   table:
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set(),
        Set(),
        Map()
      )
    ))
  }

  test("parse table singleton target") {
    parseSpec(
      """
        | spec:
        |   table:
        |     1:
        |       \alpha: 1
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set(),
        Set(),
        Map("1" -> Map("\\alpha" -> Set("1")))
      )
    ))
  }

  test("parse table multiple targets") {
    parseSpec(
      """
        | spec:
        |   table:
        |     1:
        |       \alpha: [1, 2]
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set(),
        Set(),
        Map("1" -> Map("\\alpha" -> Set("1", "2")))
      )
    ))
  }

  test("controllable null") {
    parseSpec(
      """
        | spec:
        |   controllable:
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set(),
        Set(),
        Map()
      )
    ))
  }

  test("controllable singleton") {
    parseSpec(
      """
        | spec:
        |   controllable: a
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set("a"),
        Set(),
        Map()
      )
    ))
  }

  test("controllable") {
    parseSpec(
      """
        | spec:
        |   controllable: [a, b]
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set("a", "b"),
        Set(),
        Map()
      )
    ))
  }

  test("observable null") {
    parseSpec(
      """
        | spec:
        |   observable:
        |   table:
        |     1:
        |       a:
        |       b:
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set(),
        Set("a", "b"),
        Map()
      )
    ))
  }

  test("observable singleton") {
    parseSpec(
      """
        | spec:
        |   observable: c
        |   table:
        |     1:
        |       a:
        |       b:
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set("c"),
        Set(),
        Map()
      )
    ))
  }

  test("observable") {
    parseSpec(
      """
        | spec:
        |   observable: [c, d]
        |   table:
        |     1:
        |       a:
        |       b:
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set(),
        Set("c", "d"),
        Map()
      )
    ))
  }

  test("epsilon null") {
    parseSpec(
      """
        | spec:
        |   epsilon:
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set(),
        Set(),
        Set(),
        Map()
      )
    ))
  }

  test("epsilon singleton") {
    parseSpec(
      """
        | spec:
        |   observable: e
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set("e"),
        Set(),
        Set(),
        Map()
      )
    ))
  }

  test("epsilon") {
    parseSpec(
      """
        | spec:
        |   observable: [e, epsilon, "\epsilon"]
        |""".stripMargin) should be
    Success(new FSASpec(
      nameDft,
      layouterDft,
      new Spec(
        Set(),
        Set(),
        Set("e", "epsilon", "\\epsilon"),
        Set(),
        Set(),
        Map()
      )
    ))
  }
}

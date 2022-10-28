package IDES3script


import io.vavr.collection.{HashMap => VHashMap}
import java.util.{Map => JMap}

import scala.util.Try
import scala.jdk.CollectionConverters._
import cats.data.Validated._
import cats.data.ValidatedNec
import cats.implicits._
import FSASpec.Spec._
import Utils._
import ides.api.model.fsa.{FSAModel, FSAState}
import ides.api.model.supeventset.SupervisoryEvent
import ides.api.plugin.model.ModelManager
import io.vavr.control.Option

import scala.collection.immutable.HashSet

class FSASpec (
                val name : String,
                val layouter : String,
                val spec : FSASpec.Spec) {
  override def equals(that: Any): Boolean
  = that match {
    case that: FSASpec =>
      if (this eq that) true
      else {
        name == that.name &&
        layouter == that.layouter &&
        spec == that.spec
      }
    case _ => false
  }

  def toFSAModel(): FSAModel = {
    val fsa = ModelManager
      .instance
      .createModel(classOf[FSAModel], name)

    val table = spec.table

    // String -> FSAState
    // add states to model
    val stateTbl: Map[String, FSAState] = table
      .keySet
      .union(spec.startStates)
      .union(spec.markedStates)
      .union(table
        .values
        .flatMap(_.values)
        .flatten
        .toSet
      )
      .map((key: String) => {
        Log.logDebug("State: " + key)
        val s = fsa.assembleState
        s.setName(key)
        if (spec.startStates.contains(key))
          s.setInitial(true)
        if (spec.markedStates.contains(key))
          s.setMarked(true)
        //((CircleNodeLayout)s.getAnnotation("layout")).setColor(Color.red);
        fsa.add(s)
        (key, s)
      })
      .toMap

    // {(from, event, to)}
    val trans: Set[(String, String, String)] = table
      .flatMap({ case (from: String, actObjs: Map[String, Set[String]]) =>
        actObjs
          .flatMap({ case (symbol: String, tos: Set[String]) =>
            tos
              .map((to: String) => {
                (from, symbol, to)
              })
          })
      })
      .toSet

    // String -> Event
    // add events to model
    val eventTbl: Map[String, SupervisoryEvent] = trans
      .map(_._2)
      .diff(spec.epsilon)
      .map(symbol => {
        val event = fsa.assembleEvent(symbol)

        if (spec.controllable.contains(event.getSymbol))
          event.setControllable(true)
        else
          event.setControllable(false)

        if (spec.observable.contains(event.getSymbol))
          event.setObservable(true)
        else
          event.setObservable(false)

        fsa.add(event)
        (symbol, event)
      })
      .toMap

    // add transitions to model
    trans
      .foreach({ case (from, symbol, to) =>
        val trans = if (spec.epsilon.contains(symbol)) {
          fsa.assembleEpsilonTransition(
            stateTbl(from).getId,
            stateTbl(to).getId
          )} else {
            fsa.assembleTransition(
              stateTbl(from).getId,
              stateTbl(to).getId,
              eventTbl(symbol).getId
          )}

        fsa.add(trans)
      })

    fsa
  }

  def toMap: Map[String, Any] =
    Map(
      "name" -> name,
      "layouter" -> layouter,
      "spec" -> spec
    )

  override def toString: String = {
    toMap.toString
  }
}

object FSASpec {
  class Spec(
              val startStates: Set[String],
              val markedStates: Set[String],
              val epsilon: Set[String],
              val controllable: Set[String],
              val observable: Set[String],
              val table: Map[String, Map[String, Set[String]]]) {
    override def equals(that: Any): Boolean
    = that match {
      case that: Spec =>
        if (this eq that) true
        else {
          startStates == that.startStates &&
          markedStates == that.markedStates &&
          epsilon == that.epsilon &&
          table == that.table
        }
      case _ => false
    }

    def toMap: Map[String, Any] =
      Map(
        "startStates" -> startStates,
        "markedStates" -> markedStates,
        "epsilon" -> epsilon,
        "table" -> table
      )

    override def toString: String = {
      toMap.toString
    }
  }

  object Spec {
    val nameDft = "untitled"
    val layouterDft: String = null
    val emptyspec = new Spec(
      Set(),
      Set(),
      Set(),
      Set(),
      Set(),
      Map[String, Map[String, Set[String]]]()
    )

    def validate(obj: Any): ValidatedNec[String, Spec] = {
      Try(obj)
        .map {
          case null => new java.util.HashMap()
          case v => v
        }
        .map(_.asInstanceOf[JMap[_, _]])
        .toEither
        .map(map => VHashMap.ofAll[Any,Any](map))
        .map(map => {
          val starts: ValidatedNec[String, Set[String]] =
            Utils.castSet[String](map
              .get("start states")
              .orElse(map.get("start state"))
              .getOrNull()
            )

          val marked: ValidatedNec[String, Set[String]] =
            Utils.castSet[String](map
              .get("marked states")
              .orElse(map.get("marked state"))
              .getOrNull()
            )

          val epsilon: ValidatedNec[String, Set[String]] =
            Utils.castSet[String](map
              .get("epsilon")
              .getOrNull()
            )

          val table: ValidatedNec[String, Map[String, Map[String, Set[String]]]] =
            parseSpec(map
              .get("table")
              .getOrNull()
            )

          val events: Set[String] =
            table match {
              case Valid(table) =>
                table.values.flatMap(map => map.keys).toSet
              case Invalid(_) => Set()
            }


          // events are uncontrollable by default
          val controllable: ValidatedNec[String, Set[String]] =
            Utils.castSet[String](map
              .get("controllable")
              .getOrElse(Set())
            )

          // events are observable by default
          val observable: ValidatedNec[String, Set[String]] =
            Utils.castSet[String](map
              .get("observable")
              .getOrElse(events)
            )

          (starts, marked, epsilon, controllable, observable, table).mapN(new Spec(_, _, _, _, _, _))
        })
        .fold(
          _ => ("spec cannot be understand as a map: " + obj.getClass).invalidNec,
          x => x
        )
    }

    def parseSpec (obj: Any)
    : ValidatedNec[String, Map[String, Map[String, Set[String]]]] =
      obj match {
        case null =>
          Map[String, Map[String, Set[String]]]().validNec
        case map: JMap[_, _] =>
          map
            .asScala
            .toMap
            .map({ case (k1, v1) =>
              val kv : ValidatedNec[String, String] = castString(k1) match {
                case Left(_) => s"key $k1 is not a string".invalidNec
                case Right(s) => s.validNec
              }

              // add type annotation to avoid
              // ValidatedNec[String, Map[_ <: String, Set[String]]]
              val vv : ValidatedNec[String, Map[String, Set[String]]] = v1 match {
                case null =>
                  Map[String, Set[String]]().validNec
                case map: JMap[_, _] =>
                  map
                    .asScala
                    .toMap
                    .map({ case (k2, v2) =>
                      val kv: ValidatedNec[String, String] = castString(k2) match {
                        case Left(_) => s"key $k1.$k2 is not a string".invalidNec
                        case Right(s) => s.validNec
                      }

                      val vv: ValidatedNec[String, Set[String]] = castSet[String](v2)

                      (kv, vv).mapN({ case (k, v) => (k, v) })
                    })
                    .toList
                    .sequence
                    .map(_.toMap)
                case _ =>
                  s"Value of $k1 is of type ${k1.getClass}, but Map is expected".invalidNec
              }

              (kv, vv).mapN({ case (k, v) => (k, v) })
            })
            .toList
            .sequence
            .map(_.toMap)
        case _ =>
          ("Unrecognized type: " + obj.getClass).invalidNec
      }
  }

  def validate(obj: Any): ValidatedNec[String, FSASpec] = {
    Try(obj)
      .map(_.asInstanceOf[JMap[AnyRef, AnyRef]])
      .toEither
      .map(map => map.asScala)
      .map(map => {
        val name: ValidatedNec[String, String] =
          Utils.castString(
            map.getOrElse("name", nameDft)
          )
          .fold(
            _ => "name is not a string".invalidNec,
            _.validNec
          )

        val layouter: ValidatedNec[String, String] =
          map
            .get("layouter")
            .toRight(layouterDft)
            .fold(
              validNec,
              Utils
                .castString(_)
                .fold(
                  _ => "layouter is not a string".invalidNec,
                  x => x.validNec
                )
            )

        val spec: ValidatedNec[String, Spec] =
          map
            .get("spec")
            .toRight(emptyspec)
            .fold(
              validNec,
              Spec.validate(_)
            )

        (name, layouter, spec).mapN((a, b, c) => new FSASpec(a, b, c))
      })
      .fold(
        _ => ("Input cannot be understand as a map: " + obj.getClass).invalidNec,
        x => x
      )
  }
}
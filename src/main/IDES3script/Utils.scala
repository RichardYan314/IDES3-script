package IDES3script

import cats.data.ValidatedNec
import cats.implicits._

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

object Utils {
  /**
   * Cast `obj` to a string.
   *
   * Use Either instead of Option because type inference works better on Either.fold.
   *
   * @param obj
   * @return if `obj` is a string, return `Right(obj): Right[String]`;
   *         if `obj` is numeric, return `Right(obj.toString): Right[String]`;
   *         otherwise return `Left(()): Left[Unit]`.
   */
  def castString(obj: Any): Either[Unit, String]
  = obj match {
    case s: String =>
      Right(s)
    case i: java.lang.Number => // https://stackoverflow.com/a/19757213
      Right(i.toString)
    case _ =>
      Left(())
  }

//  def castString(obj: Any): Either[Unit, String]
//  = obj match {
//    case s: String =>
//      Right(s)
//    case _ if testAnyVal(obj) => // does not work
//      Right(obj.toString)
//    case _ =>
//      Left(())
//  }
//
//  //noinspection ScalaUnusedSymbol
//  def testAnyVal[T](x: T)(implicit evidence: T <:< AnyVal = null): Boolean = evidence != null

  /**
   * Cast `obj` to a set of type `T`.
   *
   * @param obj
   * @return If `obj` is null, return an empty set;
   *         if `obj` is a singleton of type `T`, return a set of only one element (viz. `obj`);
   *         if `obj` is an iterable, return a set of the same content,
   *         provided all elements of `obj` are oftype `T`.
   *         The set is returned as `ValidNec[HashSet[T]]`.
   *         Otherwise an error message is returned as an `invalidNec[String]`.   *
   */
  def castSet[T](obj: Any)(implicit tag: ClassTag[T])
  : ValidatedNec[String, Set[T]] =
    obj match {
      case null => Set[T]().validNec
      case f: T => Set(f).validNec
      case l: java.lang.Iterable[_] =>
        l.asScala.map({
          case v: T => v.validNec
          case v => s"Value $v is not of type ${tag.runtimeClass.asInstanceOf[Class[T]]}".invalidNec
        }).toList
          .sequence
          .map(Set.from)
      case _ =>
        ("Type of " + obj + " is not understandable: " + obj.getClass.getName).invalidNec
    }
}

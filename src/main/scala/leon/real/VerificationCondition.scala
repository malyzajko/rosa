/* Copyright 2013 EPFL, Lausanne */

package leon
package real

import leon.purescala.Trees._
import leon.purescala.Definitions._
import leon.purescala.Common._
import leon.utils.{Positioned}

import Approximations._


case class Spec(id: Identifier, bounds: RationalInterval, absError: Rational)


// The condition is pre => post
class VerificationCondition(val funDef: FunDef, val kind: VCKind.Value, val pre: Expr,
  val body: Expr, val post: Expr, val variables: VariablePool,
  precisions: List[Precision]) extends Positioned {

  var allFncCalls = Set[String]()

  val fncId = funDef.id.toString // not unique

  val isLoop = TreeOps.containsIteration(body)

  // (lowerBnd, upperBnd) absError
  var spec: Map[Precision, Seq[Spec]] = precisions.map(p => (p, Seq())).toMap

  // None = still unknown
  // Some(true) = valid
  // Some(false) = invalid
  var value: Map[Precision, Option[Boolean]] = precisions.map(p => (p, None)).toMap

  def this(fD: FunDef, k:VCKind.Value, pe: Expr, b: Expr, po: Expr, fncCalls: Set[String],
    vars: VariablePool, precs: List[Precision]) = {
    this(fD, k, pe, b, po, vars, precs)
    allFncCalls = fncCalls
  }

  def status(precision: Precision) : String = value(precision) match {
    case None => "unknown"
    case Some(true) => "valid"
    case Some(false) => "invalid"
  }

  var approximations: Map[Precision, List[Approximation]] =
    precisions.map(p => (p, List())).toMap

  var time : Option[Double] = None
  var counterExample : Option[Map[Identifier, Expr]] = None

  def longString: String = "vc (%s,%s): (%s && %s) -> %s".format(fncId, kind, pre, body, post)
  override def toString: String = "%s (%s)".format(fncId, kind)
}

object VCKind extends Enumeration {
  val Precondition = Value("precond.")
  val Postcondition = Value("postcond.")
  val Assert = Value("assert.")
  val SpecGen = Value("specgen")
}

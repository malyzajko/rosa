package leon
package numerics

import ceres.common._

import purescala.Trees._
import purescala.TypeTrees._
import purescala.TreeOps._
import purescala.Definitions._
import purescala.Common._
import Utils._


class Analyser(reporter: Reporter) {

  val verbose = true

  // Currently the only constraint is the full function.
  def analyzeThis(funDef: FunDef): VerificationCondition = {
    reporter.info("")
    reporter.info("-----> Analysing function " + funDef.id.name + "...")

    val vc = new VerificationCondition(funDef)
    funDef.precondition match {
      case Some(p) =>
        val collector = new VariableCollector
        collector.transform(p)
        vc.inputs = collector.recordMap
        if (verbose) reporter.info("inputs: " + vc.inputs)
        vc.precondition = Some(p)
      case None =>
        vc.precondition = Some(BooleanLiteral(true))
    }

    funDef.postcondition match {
      case Some(post) =>
        vc.toCheck = vc.toCheck :+ Constraint(
          vc.precondition.get,
          convertLetsToEquals(funDef.body.get),
          post
        )
      case None => ;
    }
    vc.localVars = allLetDefinitions(funDef.body.get).map(letDef => Variable(letDef._1))
    println("local vars: " + vc.localVars)

    vc
  }

  private def convertLetsToEquals(expr: Expr): Expr = expr match {
    case IfExpr(cond, then, elze) =>
      IfExpr(cond, convertLetsToEquals(then), convertLetsToEquals(elze))

    case Let(binder, value, body) =>
      And(Equals(Variable(binder), value), convertLetsToEquals(body))
    case _ => expr

  }

  /*
  def analyzeThis(funDef: FunDef): VerificationCondition = {
    reporter.info("")
    reporter.info("-----> Analysing function " + funDef.id.name + "...")

    val vc = new VerificationCondition(funDef)
    funDef.precondition match {
      case Some(p) =>
        val collector = new VariableCollector
        collector.transform(p)
        vc.inputs = collector.recordMap
        //if (verbose)
          reporter.info("inputs: " + vc.inputs)
        vc.precondition = Some(p)
      case None =>
        vc.precondition = Some(BooleanLiteral(true))
    }
    val body = funDef.body.get

    val start = System.currentTimeMillis
    val (resVar, funcVars, localVars, eps) = getVariables(funDef.args, allLetDefinitions(body))


    val preConstraint: Expr = vc.precondition match {
      case Some(And(exprs)) => And(exprs.map(e => constraintFromSpec(e, funcVars, resVar, eps)))
      case Some(expr) => constraintFromSpec(expr, funcVars, resVar, eps)
      case None => reporter.warning("Forgotten precondition?"); BooleanLiteral(true)
      case _ => reporter.warning("You've got a funny precondition: " + vc.precondition); BooleanLiteral(true)
    }
    if (verbose) reporter.info("preConstr: " + preConstraint)

    //body
    val (cIdeal, cActual) =
      if (!withRoundoff) bodyConstrNoRoundoff(body, funcVars ++ localVars, resVar)
      else {
        val (realC, noisyC, deltas) = bodyConstrWholeShebang(body, funcVars ++ localVars, resVar)
        (realC, And(noisyC, constrainDeltas(deltas, eps)))
      }
    if (verbose) reporter.info("\nbody constr Real : " + cIdeal)
    if (verbose) reporter.info("\nbody constr Noisy: " + cActual)

    val postConstraint: Expr = funDef.postcondition match {
      case Some(And(exprs)) => And(exprs.map(e => constraintFromSpec(e, funcVars, resVar, eps)))
      case Some(expr) => constraintFromSpec(expr, funcVars, resVar, eps)
      case None => reporter.warning("Forgotten postcondition?"); BooleanLiteral(true)
      case _ => reporter.warning("You've got a funny postcondition: " + funDef.postcondition); BooleanLiteral(true)
    }
    if (verbose) reporter.info("postConstr: " + postConstraint)
    vc.toCheck = vc.toCheck :+ Constraint(Implies(And(Seq(preConstraint, cIdeal, cActual)), postConstraint))

    vc.preConstraint = Some(preConstraint)
    vc.bodyConstraint = Some(And(cIdeal, cActual))
    vc.postConstraint = Some(postConstraint)

    // TODO: constraints for function calls, assertions and invariants

    val totalTime = (System.currentTimeMillis - start)
    vc.analysisTime = Some(totalTime)

    vc
  }*/

/*
  // For now, this is all we allow
  private def constraintFromSpec(expr: Expr, buddy: Map[Variable, Variable], ress: Variable, eps: Variable): Expr = expr match {
    case Noise(v @ Variable(id), r @ RationalLiteral(value)) =>
      if (value < Rational.zero) {
        reporter.warning("Noise must be positive.")
        Error("negative noise").setType(BooleanType)
      } else {
        LessEquals(Abs(Minus(v, buddy(v))), r)
      }

    case Noise(ResultVariable(), r @ RationalLiteral(value)) =>
      if (value < Rational.zero) {
        reporter.warning("Noise must be positive.")
        Error("negative noise").setType(BooleanType)
      } else {
        LessEquals(Abs(Minus(ress, buddy(ress))), r)
      }

    case LessThan(Variable(_), RationalLiteral(_)) | LessThan(RationalLiteral(_), Variable(_)) => expr
    case LessEquals(Variable(_), RationalLiteral(_)) | LessEquals(RationalLiteral(_), Variable(_)) => expr
    case GreaterThan(Variable(_), RationalLiteral(_)) | GreaterThan(RationalLiteral(_), Variable(_)) => expr
    case GreaterEquals(Variable(_), RationalLiteral(_)) | GreaterEquals(RationalLiteral(_), Variable(_)) => expr

    case LessThan(ResultVariable(), RationalLiteral(_)) | LessThan(RationalLiteral(_), ResultVariable()) => replace(Map(ResultVariable() -> ress), expr)
    case LessEquals(ResultVariable(), RationalLiteral(_)) | LessEquals(RationalLiteral(_), ResultVariable()) => replace(Map(ResultVariable() -> ress), expr)
    case GreaterThan(ResultVariable(), RationalLiteral(_)) | GreaterThan(RationalLiteral(_), ResultVariable()) => replace(Map(ResultVariable() -> ress), expr)
    case GreaterEquals(ResultVariable(), RationalLiteral(_)) | GreaterEquals(RationalLiteral(_), ResultVariable()) => replace(Map(ResultVariable() -> ress), expr)

    case Roundoff(v @ Variable(id)) =>
      val delta = getNewDelta
      And(Seq(Equals(buddy(v), Times(Plus(new RationalLiteral(1), delta), v)),
        LessEquals(UMinus(eps), delta),
        LessEquals(delta, eps)))

    case _=>
      reporter.warning("Dunno what to do with this: " + expr)
      Error("unknown constraint").setType(BooleanType)
  }

  // We could also do this path by path
  // And this may be doable with a Transformer from TreeOps
  private def bodyConstrNoRoundoff(expr: Expr, buddy: Map[Expr, Expr], res: Expr): (Expr, Expr) = expr match {
    case Let(id, valueExpr, rest) =>
      val letVar = Variable(id)
      val (restReal, restNoisy) = bodyConstrNoRoundoff(rest, buddy, res)
      (And(Equals(letVar, valueExpr), restReal),
      And(Equals(buddy(letVar), replace(buddy, valueExpr)), restNoisy))

    case IfExpr(cond, then, elze) =>
      val (thenReal, thenNoisy) = bodyConstrNoRoundoff(then, buddy, res)
      val (elseReal, elseNoisy) = bodyConstrNoRoundoff(elze, buddy, res)

      val noisyCond = replace(buddy, cond)
      ( Or(And(cond, thenReal), And(Not(cond), elseReal)),
        Or(And(noisyCond, thenNoisy), And(Not(noisyCond), elseNoisy)))

    case UMinus(_) | Plus(_, _) | Minus(_, _) | Times(_, _) | Division(_, _) | FunctionInvocation(_, _) =>
      (Equals(res, expr), Equals(buddy(res), replace(buddy, expr)))

    case _=>
      reporter.warning("Dropping instruction: " + expr + ". Cannot handle it.")
      println(expr.getClass)
      (BooleanLiteral(true), BooleanLiteral(true))
  }

  // We separate the constraints on deltas from the rest for readability.
  //@return (real-valued constr, noisy constrs, deltas)
  private def bodyConstrWholeShebang(expr: Expr, buddy: Map[Expr, Expr], res: Expr):
    (Expr, Expr, List[Variable]) = expr match {
    case Let(id, valueExpr, rest) =>
      val letVar = Variable(id)
      val (restReal, restNoisy, restDeltas) = bodyConstrWholeShebang(rest, buddy, res)

      val (rndExpr, deltas) = addRndoff(replace(buddy, valueExpr))

      (And(Equals(letVar, valueExpr), restReal), And(Equals(buddy(letVar), rndExpr), restNoisy),
        restDeltas ++ deltas)

    case IfExpr(cond, then, elze) =>
      val (thenReal, thenNoisy, thenDeltas) = bodyConstrWholeShebang(then, buddy, res)
      val (elseReal, elseNoisy, elseDeltas) = bodyConstrWholeShebang(elze, buddy, res)

      val (noisyCond, deltas) = addRndoff(replace(buddy, cond))
      ( Or(And(cond, thenReal), And(Not(cond), elseReal)),
        Or(And(noisyCond, thenNoisy), And(Not(noisyCond), elseNoisy)),
        thenDeltas ++ elseDeltas ++ deltas)

    case UMinus(_) | Plus(_, _) | Minus(_, _) | Times(_, _) | Division(_, _) | FunctionInvocation(_, _) =>
      val (rndExpr, deltas) = addRndoff(replace(buddy, expr))
      (Equals(res, expr), Equals(buddy(res), rndExpr), deltas)

    case _=>
      reporter.warning("Dropping instruction: " + expr + ". Cannot handle it.")
      println(expr.getClass)
      (BooleanLiteral(true), BooleanLiteral(true), List())
  }


  // @return (constraint, deltas) (the expression with added roundoff, the deltas used)
  private def addRndoff(expr: Expr): (Expr, List[Variable]) = expr match {
    case Plus(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      val u = Plus(xExpr, yExpr)
      val (rndoff, delta) = getRndoff(u)

      (Plus(u, rndoff), xDeltas ++ yDeltas ++ List(delta))

    case Minus(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      val u = Minus(xExpr, yExpr)
      val (rndoff, delta) = getRndoff(u)
      (Plus(u, rndoff), xDeltas ++ yDeltas ++ List(delta))

    case Times(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      val u = Times(xExpr, yExpr)
      val (rndoff, delta) = getRndoff(u)
      (Plus(u, rndoff), xDeltas ++ yDeltas ++ List(delta))

    case Division(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      val u = Division(xExpr, yExpr)
      val (rndoff, delta) = getRndoff(u)
      (Plus(u, rndoff), xDeltas ++ yDeltas ++ List(delta))

    case UMinus(x) =>
      val (xExpr, xDeltas) = addRndoff(x)
      (UMinus(xExpr), xDeltas)

    case LessEquals(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      (LessEquals(xExpr, yExpr), xDeltas ++ yDeltas)

    case LessThan(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      (LessEquals(xExpr, yExpr), xDeltas ++ yDeltas)

    case GreaterEquals(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      (LessEquals(xExpr, yExpr), xDeltas ++ yDeltas)

    case GreaterThan(x, y) =>
      val (xExpr, xDeltas) = addRndoff(x)
      val (yExpr, yDeltas) = addRndoff(y)
      (LessEquals(xExpr, yExpr), xDeltas ++ yDeltas)

    case v: Variable => (v, List())

    case r: RationalLiteral => (r, List())

    case fnc: FunctionInvocation => (fnc, List())
    case _=>
      reporter.warning("Cannot add roundoff to: " + expr)
      (Error(""), List())

  }
*/
/*
object Analyser {

  // whether we consider also roundoff errors
  val withRoundoff = true

  private var deltaCounter = 0
  def getNewDelta: Variable = {
    deltaCounter = deltaCounter + 1
    Variable(FreshIdentifier("#delta_" + deltaCounter)).setType(RealType)
  }*/

  /*def getFreshRndoffMultiplier: (Expr, Variable) = {
    val delta = getNewDelta
    (Plus(new RationalLiteral(1), delta) , delta)
  }*/
/*
  def getRndoff(expr: Expr): (Expr, Variable) = {
    val delta = getNewDelta
    (Times(expr, delta), delta)
  }

  def constrainDeltas(deltas: List[Variable], eps: Variable): Expr = {
    val constraints = deltas.map(delta =>
      And(LessEquals(UMinus(eps), delta),
        LessEquals(delta, eps))
      )
    And(constraints ++ Seq(Equals(eps, RationalLiteral(unitRndoff))))
  }

  def getVariables(args: Seq[VarDecl], lets: List[(Identifier, Expr)]):
    (Variable, Map[Variable, Variable], Map[Variable, Variable], Variable) = {
    val resVar = Variable(FreshIdentifier("res")).setType(RealType)
    val machineEps = Variable(FreshIdentifier("#eps")).setType(RealType)

    var funcVars: Map[Variable, Variable] =
      args.foldLeft(Map(resVar -> Variable(FreshIdentifier("#res_0")).setType(RealType)))(
        (map, nextArg) => map + (Variable(nextArg.id).setType(RealType) -> Variable(FreshIdentifier("#"+nextArg.id.name+"_0")).setType(RealType))
      )
    var localVars: Map[Variable, Variable] = lets.foldLeft(Map[Variable, Variable]())(
      (map, defpair) => map + (Variable(defpair._1).setType(RealType) ->
          Variable(FreshIdentifier("#"+defpair._1.name+"_0")).setType(RealType))
    )
    (resVar, funcVars, localVars, machineEps)
  }
}
*/



  // It is complete, if the result is bounded below and above and the noise is specified.
/*  private def isComplete(post: Expr): Boolean = {
    post match {
      case and @ And(args) =>
        val variableBounds = Utils.getVariableBounds(and)
        val noise = TreeOps.contains(and, (
          a => a match {
            case Noise(ResultVariable()) => true
            case _ => false
          }))
        noise && variableBounds.contains(Variable(FreshIdentifier("#res")))

      case _ =>
        reporter.warning("Unsupported type of postcondition: " + post)
        false
    }
  }
*/

}

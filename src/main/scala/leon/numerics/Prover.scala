package leon
package numerics

import ceres.common.{Rational, RationalInterval}

import purescala.Common._
import purescala.Definitions._
import purescala.Trees._
import purescala.TreeOps._
import purescala.TypeTrees._

import affine.{XFloat, XFloatConfig}
import affine.XFloat._

import Utils._

import Sat._
import Valid._
import ApproximationType._
import Precision._


class Prover(reporter: Reporter, ctx: LeonContext, program: Program, vcMap: Map[FunDef, VerificationCondition], precision: Precision) {
  val verbose = false
  val solver = new NumericSolver(ctx, program)
  val postInliner = new PostconditionInliner(reporter)
  val fullInliner = new FullInliner(reporter, vcMap)

  val unitRoundoff = getUnitRoundoff(precision)

  def check(vc: VerificationCondition) = {
    reporter.info("")
    reporter.info("----------> checking VC of " + vc.funDef.id.name)

    val start = System.currentTimeMillis
    for (c <- vc.allConstraints) {
      reporter.info("----------> checking constraint: " + c.description)
      if (verbose) {println("pre: " + c.pre); println("body: " + c.body); println("post: " + c.post)}

      while (c.hasNextApproximation && !c.solved) {
        val next = c.getNextApproxType.get
        reporter.info("Computing approximation: " + next)
        val approx = getNextApproximation(next, c, vc.inputs)
        c.approximations = c.approximations :+ approx
        c.overrideStatus(checkWithZ3(approx, vc.allVariables))
        reporter.info("RESULT: " + c.status)
        //if (!c.model.isEmpty) reporter.info(c.model.get)

      }
    }

    val totalTime = (System.currentTimeMillis - start)
    vc.verificationTime = Some(totalTime)
  }

  //case class APath(pathCondition: Expr, idealBody: Expr, idealCnst: Expr, actualBody: Expr, actualCnst: Expr)
  //case class ConstraintApproximation(pre: Expr, body: Set[APath], post: Expr, vars: Set[Variable], tpe: ApproximationType)

  // TODO: we can cache some of the body transforms and reuse for AA...
  def getNextApproximation(tpe: ApproximationType, c: Constraint, inputs: Map[Variable, Record]): ConstraintApproximation = tpe match {
    /* ******************
       NO APPROXIMATION    
    * ******************* */
    case Uninterpreted_None =>
      val paths = collectPaths(c.body).map(p => getAPath(p))
      ConstraintApproximation(c.pre, paths, c.post, Set.empty, tpe)

    case PostInlining_None =>
      val (newPre, newBody, newPost, vars) = postInliner.inlinePostcondition(c.pre, c.body, c.post)
      val paths = collectPaths(newBody).map(p => getAPath(p))
      ConstraintApproximation(newPre, paths, newPost, vars, tpe)

    case FullInlining_None =>
      val (newPre, newBody, newPost, vars) = fullInliner.inlineFunctions(c.pre, c.body, c.post)
      val paths = collectPaths(newBody).map(p => getAPath(p))
      ConstraintApproximation(newPre, paths, newPost, vars, tpe)

    /* ******************
       Full APPROXIMATION    
    * ******************* */
    case NoFncs_AA =>
      val (newConstraint, values) = approximatePaths(c.paths, c.pre, inputs)
      println("AA computed: " + newConstraint)
      // TODO: simplify constraint, we don't need all the info
      val paths = collectPaths(c.body).map(p => getAPath(p).updateNoisy(True, True))
      val cnstr = ConstraintApproximation(newConstraint, paths, c.post, Set.empty, tpe)
      cnstr.values = values
      cnstr

    case PostInlining_AA =>
      val (newPre, newBody, newPost, vars) = postInliner.inlinePostcondition(c.pre, c.body, c.post)
      val (newConstraint, values) = approximatePaths(collectPaths(newBody), newPre, getVariableRecords(newPre))
      val paths = collectPaths(newBody).map(p => getAPath(p).updateNoisy(True, True))
      val cnstr = ConstraintApproximation(newConstraint, paths, newPost, vars, tpe)
      cnstr.values = values
      cnstr   

    case FullInlining_AA =>
      val (newPre, newBody, newPost, vars) = postInliner.inlinePostcondition(c.pre, c.body, c.post)
      val (newConstraint, values) = approximatePaths(collectPaths(newBody), newPre, getVariableRecords(newPre))
      val paths = collectPaths(newBody).map(p => getAPath(p).updateNoisy(True, True))
      val cnstr = ConstraintApproximation(newConstraint, paths, newPost, vars, tpe)
      cnstr.values = values
      cnstr

    // TODO: next step: check every path separately

      // TODO: If neither work, do partial approx.
  }




  private def checkWithZ3(ca: ConstraintApproximation, parameters: Seq[Variable]): (Option[Valid], Option[Map[Identifier, Expr]]) = {
    val (resVar, eps, buddies) = getVariables(parameters ++ ca.vars)
    val trans = new NumericConstraintTransformer(buddies, resVar, eps, RoundoffType.RoundoffMultiplier, reporter)
    val precondition = trans.transformCondition(ca.pre)
    val postcondition = trans.transformCondition(ca.post)

    var (idealPart, actualPart) = (Seq[Expr](), Seq[Expr]())
    for(path <- ca.paths) {
      val (aI, nI) = trans.transformBlock(path.idealBody)
      idealPart = idealPart :+ And(And(path.pathCondition, path.idealCnst), aI)
      val (aN, nN) = trans.transformBlock(path.actualBody)
      actualPart = actualPart :+ And(And(trans.getNoisyCondition(path.pathCondition), path.actualCnst), nN)
    }
        
    val body = And(Or(idealPart), Or(actualPart))
    // This is to make Z3 gives us also the error
    val resultError = Equals(getNewResErrorVariable, Minus(resVar, buddies(resVar)))
    val machineEpsilon = Equals(eps, RationalLiteral(unitRoundoff))
    val toCheck = Implies(And(precondition, And(body, And(resultError, machineEpsilon))), postcondition)
    println("toCheck: " + toCheck)

    val firstTry = if (reporter.errorCount == 0 && sanityCheck(precondition, body)) {
      val (res, model) = solver.checkValid(toCheck)
      (Some(res), model)
    } else {
      (None, None)
    }

    firstTry match {
      case (Some(VALID), _) => firstTry
      case _ => // try again
        val paths = idealPart.zip(actualPart)
        for ((i, a) <- paths) {
          val cnstr = Implies(And(precondition, And(And(i, a), And(resultError, machineEpsilon))), postcondition)
          println("checking path: " + And(i, a))
          val (res, model) = solver.checkValid(toCheck)
          println("with result: " + res)
          if (res != VALID) {
            reporter.info("path could not be proven: " + And(i, a))
            return (Some(res), model)
          }
        }
    }
    (Some(VALID), None)
  }

  private def computeApproximation(paths: Set[Path], precondition: Expr, inputs: Map[Variable, Record]) = {
    for (path <- paths) {
      val pathCondition = And(path.condition, filterPreconditionForBoundsIteration(precondition))
      if (sanityCheck(pathCondition)) {  // If this implies false, range tightening fails
        // The condition given to the solver is the real(ideal)-valued one, since we use Z3 for the real part only.
        val config = XFloatConfig(reporter, solver, pathCondition, precision, unitRoundoff)
        val (variables, indices) = variables2xfloats(inputs, config)
        solver.countTimeouts = 0
        path.values = inXFloats(path.expression, variables, config) -- inputs.keys
        reporter.info("Timeouts: " + solver.countTimeouts)
        //println("path values: " + path.values)
        path.indices= indices

      } else {
        reporter.warning("skipping path " + path)
        // TODO: what to do here? we only checked the ideal part is impossible,
        // but the floating-point part may still be possible
        // although this would be quite the strange scenario...
      }
    }
  }

  // Computes one constraint that overapproximates the paths given.
  private def approximatePaths(paths: Set[Path], pre: Expr, inputs: Map[Variable, Record]): (Expr, Map[Expr, (RationalInterval, Rational)]) = {
    computeApproximation(paths, pre, inputs)
    //println("approximation: " + paths.head.values)
    val approx = mergeRealPathResults(paths)
    //println("merged: " + approx)
    val newConstraint = constraintFromResults(approx)
    (newConstraint, approx)
  }

  private def getAPath(path: Path): APath = APath(path.condition, And(path.expression), True, And(path.expression), True)
  


  // Returns a map from all variables to their final value, including local vars
  private def inXFloats(exprs: List[Expr], vars: Map[Expr, XFloat], config: XFloatConfig): Map[Expr, XFloat] = {
    var currentVars: Map[Expr, XFloat] = vars

    for (expr <- exprs) expr match {
      case Equals(variable, value) =>
        try {
          val computedValue = eval(value, currentVars, config)
          //println("computedValue: " + computedValue)
          currentVars = currentVars + (variable -> computedValue)
          //println("currentVars: " + currentVars)
        } catch {
          case UnsupportedFragmentException(msg) => reporter.error(msg)
        }

      case BooleanLiteral(true) => ;
      case _ =>
        reporter.error("AA cannot handle: " + expr)
    }

    currentVars
  }

  // Evaluates an arithmetic expression
  private def eval(expr: Expr, vars: Map[Expr, XFloat], config: XFloatConfig): XFloat = expr match {
    case v @ Variable(id) => vars(v)
    case RationalLiteral(v) => XFloat(v, config)
    case IntLiteral(v) => XFloat(v, config)
    case UMinus(rhs) => - eval(rhs, vars, config)
    case Plus(lhs, rhs) => eval(lhs, vars, config) + eval(rhs, vars, config)
    case Minus(lhs, rhs) => eval(lhs, vars, config) - eval(rhs, vars, config)
    case Times(lhs, rhs) => eval(lhs, vars, config) * eval(rhs, vars, config)
    case Division(lhs, rhs) => eval(lhs, vars, config) / eval(rhs, vars, config)
    case Sqrt(t) => eval(t, vars, config).squareRoot
    case _ =>
      throw UnsupportedFragmentException("AA cannot handle: " + expr)
      null
  }


  // if true, we're sane
  private def sanityCheck(pre: Expr, body: Expr = BooleanLiteral(true)): Boolean = {
    val sanityCondition = And(pre, body)
    solver.checkSat(sanityCondition) match {
      case (SAT, model) =>
        reporter.info("Sanity check passed! :-)")
        //reporter.info("model: " + model)
        true
      case (UNSAT, model) =>
        reporter.warning("Not sane! " + sanityCondition)
        false
      case _ =>
        reporter.warning("Sanity check failed! ")// + sanityCondition)
        false
    }
  }

  private def getVariables(variables: Seq[Variable]): (Variable, Variable, Map[Expr, Expr]) = {
    val resVar = Variable(FreshIdentifier("#ress")).setType(RealType)
    val machineEps = Variable(FreshIdentifier("#eps")).setType(RealType)

    var buddies: Map[Expr, Expr] =
      variables.foldLeft(Map[Expr, Expr](resVar -> Variable(FreshIdentifier("#res_0")).setType(RealType)))(
        (map, nextVar) => map + (nextVar -> Variable(FreshIdentifier("#"+nextVar.id.name+"_0")).setType(RealType))
      )
    (resVar, machineEps, buddies)
  }


  private def filterPreconditionForBoundsIteration(expr: Expr): Expr = expr match {
    case And(args) => And(args.map(a => filterPreconditionForBoundsIteration(a)))
    case Noise(e, f) => BooleanLiteral(true)
    case Roundoff(e) => BooleanLiteral(true)
    case _ => expr
  }

  private def filterDeltas(expr: Expr): Expr = expr match {
    case And(args) => And(args.map(a => filterDeltas(a)))
    case LessEquals(Variable(id1), Variable(id2)) if (id1.toString.contains("#delta_") && id2.toString == "#eps") =>
      //println("filtering out: " + expr)
      True
    case LessEquals(UMinus(Variable(id1)), Variable(id2)) if (id1.toString == "#eps" && id2.toString.contains("#delta_")) =>
      //println("filtering out: " + expr)
      True

    case _ => expr
  }

  


/*def addSpecs(vc: VerificationCondition): VerificationCondition = {
    //val start = System.currentTimeMillis
    // if there are constraints, then those have already been handled, only deal with VCs without post
    if(!vc.specConstraint.get.approximated) {
      //computeApproximation(vc.specConstraint.get, vc.inputs)
    }

    //val totalTime = (System.currentTimeMillis - start)
    //vc.verificationTime = Some(totalTime)
    vc
  }*/

}

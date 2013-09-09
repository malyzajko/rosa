/* Copyright 2013 EPFL, Lausanne */

package leon
package real

import purescala.Trees._
import purescala.Definitions._
import purescala.TreeOps._

import real.TreeOps._
import Sat._
import FncHandling._
import ArithmApprox._
import PathHandling._
import Rational._


class Prover(ctx: LeonContext, options: RealOptions, prog: Program, fncs: Map[FunDef, Fnc], verbose: Boolean = false) {
  val reporter = ctx.reporter
  val solver = new RealSolver(ctx, prog, options.z3Timeout)
  
  def check(vcs: Seq[VerificationCondition]): Precision = {
    options.precision.find( precision => {
      reporter.info("******** precision: %s *************".format(precision))

      for (vc <- vcs) {
        reporter.info("Verification condition (%s) ==== %s ====".format(vc.kind, vc.fncId))
        println("pre: " + vc.pre)
        println("body: " + vc.body)
        println("post: " + vc.post)
        reporter.info("Trying with approximation")
        val start = System.currentTimeMillis
        var spec: Option[Spec] = None

        // TODO: filter out those that are not applicable
        // TODO: this we also don't need to do for all precisions each time
        // TODO: some combinations don't work: e.g. Uninterpreted & JustFloat
        val approximations = List(//ApproxKind(Uninterpreted, Merging, Z3Only), 
                                  ApproxKind(Inlining, Merging, JustFloat))
                                  //ApproxKind(Uninterpreted, Pathwise, JustFloat))
        
        // TODO: re-use some of the approximation work across precision?
        approximations.find(aKind => {
          reporter.info("  - " + aKind)

          try {
            val currentApprox = getApproximation(vc, aKind, precision)
            spec = merge(spec, currentApprox.spec)
          
            //if (verbose) println(currentApprox.cnstrs)
            checkValid(currentApprox, vc.variables, precision) match {
              case Some(true) =>
                reporter.info("==== VALID ====")
                vc.value += (precision -> Some(true))
                true
              case Some(false) =>
                // TODO: figure out if we can find invalid
                reporter.info("=== INVALID ===")
                true
              case None =>
                reporter.info("---- Unknown ----")
                false
            }
          } catch {
            case e: java.lang.ArithmeticException =>
              reporter.warning("Failed to compute approximation: " + e.getMessage)
              false
            case e: ArithmeticException =>
              reporter.error("This error should not have happened. Please report.")
              false
          }

        }) match {
          case None =>
          case _ =>
        }
        if (verbose) println("generated spec: " + spec)
        vc.spec += (precision -> spec)
      
        val end = System.currentTimeMillis
        vc.time = Some(end - start)
      }
      vcs.forall( vc => {
        vc.value(precision) match {
          case None => false 
          case _ => true
        }
      })

    }) match {
      case Some(p) => p
      case None => options.precision.last
    }
    
  }

  def checkValid(app: Approximation, variables: VariablePool, precision: Precision): Option[Boolean] = {
    if (verbose) println("\napprox: " + app.cnstrs)

    // I think we can keep one
    val transformer = new LeonToZ3Transformer(variables)
    var valid: Option[Boolean] = Some(true)

    for (((constraint, sanityExpr), index) <- app.cnstrs.zip(app.sanityChecks).view.zipWithIndex) {
      //println("constraint: " + constraint)

      val (z3constraint, sane) = precision match {
        case FPPrecision(bts) =>
          (massageArithmetic(transformer.getZ3ExprForFixedpoint(constraint)),
            sanityCheck(transformer.getZ3ExprForFixedpoint(sanityExpr)))
        case _ =>
          (massageArithmetic(transformer.getZ3Expr(constraint, precision)),
            sanityCheck(transformer.getZ3Expr(sanityExpr, precision)))
      }
      if (verbose) println("\n z3constraint ("+index+"): " + z3constraint)

      if (reporter.errorCount == 0 && sane)
        solver.checkSat(z3constraint) match {
          case (UNSAT, _) =>;
          case (SAT, model) =>
            //println("Model found: " + model)
            // TODO: print the models that are actually useful, once we figure out which ones those are
            // Return Some(false) if we have a valid model
            valid = None
          case _ =>
            valid = None
        }
      else
        valid = None
    }
    valid
  }

  // DOC: we only support function calls in fnc bodies, not in pre and post
  def getApproximation(vc: VerificationCondition, kind: ApproxKind, precision: Precision): Approximation = {
    val postInliner = new PostconditionInliner
    val fncInliner = new FunctionInliner(fncs)

    val (preFnc, bodyFnc, postFnc) = kind.fncHandling match {
      case Uninterpreted =>
        (vc.pre, vc.body, vc.post)

      case Postcondition =>
        (vc.pre, postInliner.transform(vc.body), vc.post)

      case Inlining =>
        (vc.pre, fncInliner.transform(vc.body), vc.post)
    }
    if (verbose)
      println("\nafter FNC handling:\npre: %s\nbody: %s\npost: %s".format(preFnc,bodyFnc,postFnc))


    val (pre, body, post): (Expr, Set[Path], Expr) = kind.pathHandling match {
      case Pathwise =>
        ( preFnc,
          getPaths(bodyFnc).map(p => Path(p.condition, And(p.body, idealToActual(p.body, vc.variables)))), 
          postFnc )
      case Merging =>
        ( preFnc,
          Set(Path(True, And(bodyFnc, idealToActual(bodyFnc, vc.variables)))),
          postFnc )
    }
    if (verbose)
      println("\nafter PATH handling:\npre: %s\nbody: %s\npost: %s".format(pre,body.mkString("\n"),post))

    
    kind.arithmApprox match {
      case Z3Only =>
        var approx = Seq[Expr]()
        var sanity = Seq[Expr]()
        for (path <- body) {
          approx :+= And(And(pre, And(path.condition, path.body)), negate(post))  // Implies(And(vc.pre, vc.body), vc.post)))
          sanity :+= And(pre, And(path.condition, path.body))
        }
        Approximation(kind, approx, sanity, None)

      case JustFloat =>
        var approx = Seq[Expr]()
        var sanity = Seq[Expr]()
        var spec: Option[Spec] = None
  
        for ( path <- body ) {
          // Hmm, this uses the same solver as the check...
          val transformer = new FloatApproximator(reporter, solver, precision, And(pre, path.condition), vc.variables)
          val (newBody, newSpec) = transformer.transformWithSpec(path.body)
          spec = merge(spec, newSpec)
          if (verbose) println("body after approx: " + newBody)
          approx :+= And(And(pre, And(path.condition, newBody)), negate(post))
          sanity :+= And(pre, newBody)
        }
        Approximation(kind, approx, sanity, spec)

      case FloatNRange =>
        Approximation(kind, List(), List(), None)
    }
  }

  private def merge(currentSpec: Option[Spec], newSpec: Option[Spec]): Option[Spec] = (currentSpec, newSpec) match {
    case (Some(s1), Some(s2)) =>
      val lowerBnd = min(s1.bounds.xlo, s2.bounds.xlo)
      val upperBnd = max(s1.bounds.xhi, s2.bounds.xhi)
      val err = max(s1.absError, s2.absError)
      Some(Spec(RationalInterval(lowerBnd, upperBnd), err))
    case (None, Some(s)) => newSpec
    case _ => currentSpec
  }

  // if true, we're sane
  private def sanityCheck(pre: Expr, body: Expr = BooleanLiteral(true)): Boolean = {
    val sanityCondition = And(pre, body)
    solver.checkSat(sanityCondition) match {
      case (SAT, model) => true
      case (UNSAT, model) =>
        reporter.warning("Not sane! " + sanityCondition)
        false
      case _ =>
        reporter.info("Sanity check failed! ")// + sanityCondition)
        false
    }
  }
}
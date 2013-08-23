/* Copyright 2013 EPFL, Lausanne */

package leon
package real

import java.io.{PrintWriter, File}

import purescala.Definitions._
import purescala.Trees._
import purescala.TreeOps._
import purescala.TypeTrees._
import purescala.Common._
import purescala.ScalaPrinter

import xlang.Trees._

import real.Trees._
import real.TreeOps._

import Precision._
import VCKind._


object CompilationPhase extends LeonPhase[Program,CompilationReport] {
  val name = "Real compilation"
  val description = "compilation of real programs"

  var verbose = true
  var reporter: Reporter = null

  override val definedOptions: Set[LeonOptionDef] = Set(
    LeonValueOptionDef("functions", "--functions=f1:f2", "Limit verification to f1, f2,..."),
    LeonFlagOptionDef("simulation", "--simulation", "Run a simulation instead of verification"),
    LeonFlagOptionDef("pathSensitive", "--pathSensitive", "Do a path sensitive analysis."),
    LeonFlagOptionDef("z3only", "--z3only", "Let Z3 loose on the full constraint - at your own risk."),
    LeonValueOptionDef("z3timeout", "--z3timeout=1000", "Timeout for Z3 in milliseconds."),
    LeonValueOptionDef("precision", "--precision=single", "Which precision to assume of the underlying"+
      "floating-point arithmetic: single, double, doubledouble, quaddouble or all (finds the best one)."),
    LeonFlagOptionDef("nospecgen", "--nospecgen", "Don't generate specs.")
  )


  def run(ctx: LeonContext)(program: Program): CompilationReport = { 
    reporter = ctx.reporter
    reporter.info("Running Compilation phase")

    var fncNamesToAnalyse = Set[String]()
    val options = new RealOptions

    for (opt <- ctx.options) opt match {
      case LeonValueOption("functions", ListValue(fs)) => fncNamesToAnalyse = Set() ++ fs
      case LeonFlagOption("simulation") => options.simulation = true
      case LeonFlagOption("pathSensitive") => options.pathSensitive = true
      case LeonFlagOption("z3only") => options.z3Only = true
      case LeonValueOption("z3timeout", ListValue(tm)) => options.z3Timeout = tm.head.toLong
      case LeonValueOption("precision", ListValue(ps)) => options.precision = ps.head match {
        case "single" => List(Float32)
        case "double" => List(Float64)
        case "doubledouble" => List(DoubleDouble)
        case "quaddouble" => List(QuadDouble)
        case "all" => List(Float32, Float64, DoubleDouble, QuadDouble)
      }
      // TODO: enable this
      case LeonFlagOption("nospecgen") => options.specGen = false
      case _ =>
    }
    
    val fncsToAnalyse  = 
      if(fncNamesToAnalyse.isEmpty) program.definedFunctions
      else {
        val toAnalyze = program.definedFunctions.filter(f => fncNamesToAnalyse.contains(f.id.name))
        val notFound = fncNamesToAnalyse -- toAnalyze.map(fncDef => fncDef.id.name).toSet
        notFound.foreach(fn => reporter.error("Did not find function \"" + fn + "\" though it was marked for analysis."))
        toAnalyze
      }
        
    val vcs = analyzeThis(fncsToAnalyse).sortWith((vc1, vc2) => lt(vc1, vc2))
    if (reporter.errorCount > 0) throw LeonFatalError()
    
    if (options.simulation) {
      val simulator = new Simulator(reporter)
      val prec = if (options.precision.size == 1) options.precision.head else Float64
      for(vc <- vcs) simulator.simulateThis(vc, prec)
      new CompilationReport(List())
    } else {
      val prover = new Prover(ctx, options, program, verbose)
      val finalPrecision = prover.check(vcs)

      val newProgram = specToCode(program.id, program.mainObject.id, vcs, finalPrecision) 
      val newProgramAsString = ScalaPrinter(newProgram)
      reporter.info("Generated program with %d lines.".format(newProgramAsString.lines.length))
      //reporter.info(newProgramAsString)

      val writer = new PrintWriter(new File("generated/" + newProgram.mainObject.id +".scala"))
      writer.write(newProgramAsString)
      writer.close()
    
      new CompilationReport(vcs.sortWith((vc1, vc2) => vc1.id < vc2.id))
    }
    
  }

  private def analyzeThis(sortedFncs: Seq[FunDef]): Seq[VerificationCondition] = {
    var vcs: Seq[VerificationCondition] = Seq.empty
    
    for (funDef <- sortedFncs if (funDef.body.isDefined)) {
      reporter.info("Analysing fnc:  %s".format(funDef.id.name))
      if (verbose) println(funDef.body.get)
      
      funDef.precondition match {
        case Some(precondition) =>
          val variables = VariablePool(precondition)
          if (verbose) println("parameters: " + variables)
          if (variables.hasValidInput(funDef.args)) {
            if (verbose) println("prec. is complete, continuing")

            if (verbose) println("pre: " + precondition)
            val allFncCalls = functionCallsOf(precondition).map(invc => invc.funDef.id.toString) ++
              functionCallsOf(funDef.body.get).map(invc => invc.funDef.id.toString)

            val (fncBody, postcondition) = funDef.postcondition match {
              case Some(ResultVariable()) =>
                val posts = getInvariantCondition(funDef.body.get)
                val bodyWOLets = convertLetsToEquals(funDef.body.get)
                val body = replace(posts.map(p => (p, True)).toMap, bodyWOLets)
                (body, Or(posts))
              case Some(p) => (convertLetsToEquals(addResult(funDef.body.get)), p)

              case None => (convertLetsToEquals(addResult(funDef.body.get)), BooleanLiteral(true))
            }

            /*println("\nfncBody: " + fncBody)
            println("\npost: " + postcondition)
            println("\n body real : " + fncBody)
            println("\n body float: " + idealToActual(fncBody, variables))
            */
            // add floating-point "track"
            val body = And(fncBody, idealToActual(fncBody, variables))
            
            vcs :+= new VerificationCondition(funDef, Postcondition, precondition, body, postcondition, allFncCalls, variables)
            
            vcs.last.realFncBody = fncBody // this is clearly a hack (only for simulation)

            // TODO: vcs from assertions
            // TODO: vcs checking precondition of function calls

          } else {
            reporter.warning("Incomplete precondition! Skipping...")
          }
        case None =>
      }
    }
    vcs
  }

  private def lt(vc1: VerificationCondition, vc2: VerificationCondition): Boolean = {
    if (vc1.allFncCalls.isEmpty) true
    else if (vc2.allFncCalls.isEmpty) false
    else if (vc2.allFncCalls.contains(vc1.id)) true
    else if (vc1.allFncCalls.contains(vc2.id)) false
    else true
  }


  private def specToCode(programId: Identifier, objectId: Identifier, vcs: Seq[VerificationCondition], precision: Precision): Program = {
    var defs: Seq[Definition] = Seq.empty

    for (vc <- vcs) {
      val f = vc.funDef
      val id = f.id
      val floatType = getNonRealType(precision)
      val returnType = floatType // FIXME: check that this is actually RealType
      val args: Seq[VarDecl] = f.args.map(decl => VarDecl(decl.id, floatType))

      val funDef = new FunDef(id, returnType, args)
      funDef.body = f.body

      funDef.precondition = f.precondition

      vc.spec(precision) match {
        case Some(Spec(int, err)) =>
          funDef.postcondition = Some(And(And(LessEquals(RationalLiteral(int.xlo), ResultVariable()),
            LessEquals(ResultVariable(), RationalLiteral(int.xhi))),
            Noise(ResultVariable(), RationalLiteral(err))))
        case _ =>
      }
      
      defs = defs :+ funDef
    }
    val invariants: Seq[Expr] = Seq.empty

    val newProgram = Program(programId, ObjectDef(objectId, defs, invariants))
    newProgram
  }

  private def getNonRealType(precision: Precision): TypeTree = precision match {
    case Float64 => Float64Type
    case Float32 => Float32Type
    case DoubleDouble => FloatDDType
    case QuadDouble => FloatQDType
  }
}


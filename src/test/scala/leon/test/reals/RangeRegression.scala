/* Copyright 2009-2013 EPFL, Lausanne */

package leon
package test
package real

import leon.real.{CompilationPhase,CompilationReport,VCKind, Precision, Spec}

import org.scalatest.FunSuite

import java.io.File

import TestUtils._

class RangeRegression extends FunSuite {
  private var counter : Int = 0
  private def nextInt() : Int = {
    counter += 1
    counter
  }
  private case class Output(report : CompilationReport, reporter : Reporter)

  private def mkPipeline : Pipeline[List[String],CompilationReport] =
    leon.plugin.ExtractionPhase andThen leon.SubtypingPhase andThen leon.real.CompilationPhase

  // for now one, but who knows
  val realLibraryFiles = filesInResourceDir(
    "regression/verification/real/library", _.endsWith(".scala"))

  val currentRangesFile = filesInResourceDir(
    "regression/verification/real/ranges", _.endsWith("currentRanges.txt")).head

  val source = scala.io.Source.fromFile(currentRangesFile.getPath)
  val lines = source.getLines.toList
  source.close()
  
  val baseline = lines.map{ line => line.split(" ") match {
    case Array(fncName, a, b, err) =>
      (fncName, (a.toDouble, b.toDouble, err.toDouble))
    case _ => println("invalid input line")
      ("dummy", (0.0, 0.0, 0.0))
    }
  }.toMap
  //println("baseline: " + baseline.mkString("\n"))

  private def mkTest(file : File, leonOptions : Seq[LeonOption], forError: Boolean)(block: Output=>Unit) = {
    val fullName = file.getPath()
    val start = fullName.indexOf("regression")

    val displayName = if(start != -1) {
      fullName.substring(start, fullName.length)
    } else {
      fullName
    }

    test("%3d: %s %s".format(nextInt(), displayName, leonOptions.mkString(" "))) {
      assert(file.exists && file.isFile && file.canRead,
             "Benchmark %s is not a readable file".format(displayName))

      val ctx = LeonContext(
        settings = Settings(
          synthesis = false,
          xlang     = false,
          verify    = false,
          real = true
        ),
        options = leonOptions.toList,
        files = List(file) ++ realLibraryFiles,
        reporter = new SilentReporter
        //reporter = new DefaultReporter
      )

      val pipeline = mkPipeline

      if(forError) {
        intercept[LeonFatalError]{
          pipeline.run(ctx)(file.getPath :: Nil)
        }
      } else {

        val report = pipeline.run(ctx)(file.getPath :: Nil)

        block(Output(report, ctx.reporter))
      }
    }
  }

  private def mkIgnore(file: File) = {
    val fullName = file.getPath()
    val start = fullName.indexOf("regression")

    val displayName = if(start != -1) {
      fullName.substring(start, fullName.length)
    } else {
      fullName
    }
    ignore(displayName) {
      assert(true)
    }
  }

  private def forEachFileIn(cat : String, forError: Boolean = false)(block : Output=>Unit) {
    val fs = filesInResourceDir(
      "regression/verification/real/" + cat,
      _.endsWith(".scala"))

    for(f <- fs) {
      mkTest(f, List(LeonFlagOption("real")), forError)(block)
    }

    /*val ignoredFiles = filesInResourceDir(
      "regression/verification/real/" + cat,
      _.endsWith(".txt"))
    for(f <- ignoredFiles) {
      mkIgnore(f)
    } */   
  }
  
  forEachFileIn("ranges") { output =>
    val Output(report, reporter) = output
    
    for(vc <- report.allVCs if (vc.kind == VCKind.SpecGen)) {
      val bsLine = baseline(vc.fncId)
      val Some(Spec(bounds, absError)) = vc.spec(Precision.Float64)
      assert(bsLine._1 === bounds.xlo, "lower bound doesn't match baseline for " + vc.fncId)
      assert(bsLine._2 === bounds.xhi, "upper bound doesn't match baseline for " + vc.fncId)
      assert(bsLine._3 === absError, "error doesn't match baseline for " + vc.fncId)
       
    }
    assert(reporter.errorCount === 0)
    assert(reporter.warningCount === 0)
  }

  

}

package purescala

import purescala.Trees._
import purescala.Definitions._

object Extensions {
  sealed abstract class Extension(reporter: Reporter) {
    def description: String
    def shortDescription: String = description
  }

  abstract class Solver(reporter: Reporter) extends Extension(reporter) {
    // This can be used by solvers to "see" the programs from which the
    // formulas come. (e.g. to set up some datastructures for the defined
    // ADTs, etc.) 
    def setProgram(program: Program) : Unit = {}

    // Returns Some(true) if valid, Some(false) if invalid,
    // None if unknown.
    def solve(expression: Expr) : Option[Boolean]
  }
  
  abstract class Analyser(reporter: Reporter) extends Extension(reporter) {
    // Does whatever the analysis should. Uses the reporter to
    // signal results and/or errors.
    def analyse(program: Program) : Unit
  }

  // The rest of the code is for dynamically loading extensions

  def loadAll(reporter: Reporter) : Seq[Extension] = {
    val allNames: Seq[String] = Settings.extensionNames.split(':').map(_.trim).filter(!_.isEmpty)
    if(!allNames.isEmpty) {
      val classLoader = Extensions.getClass.getClassLoader

      val classes: Seq[Class[_]] = (for(name <- allNames) yield {
        try {
          classLoader.loadClass(name)
        } catch {
          case _ => {
            Settings.reporter.error("Couldn't load extension class " + name) 
            null
          }
        }
      }).filter(_ != null)

      classes.map(cl => {
        try {
          val cons = cl.getConstructor(classOf[Reporter])
          cons.newInstance(reporter).asInstanceOf[Extension]
        } catch {
          case _ => {
            Settings.reporter.error("Extension class " + cl.getName + " does not seem to be of a proper type.")
            null
          }
        }
      }).filter(_ != null)
    } else {
      Nil
    }
  }
}
package leon

object Main {

  def allPhases: List[LeonPhase[_, _]] = {
    List(
      plugin.ExtractionPhase,
      ArrayTransformation,
      EpsilonElimination,
      ImperativeCodeElimination,
      /*UnitElimination,*/
      FunctionClosure,
      /*FunctionHoisting,*/
      Simplificator,
      synthesis.SynthesisPhase,
      AnalysisPhase
    )
  }

  lazy val allOptions = allPhases.flatMap(_.definedOptions) ++ Set(
      LeonOptionDef("synthesis",     true,  "--synthesis          Partial synthesis or choose() constructs"),
      LeonOptionDef("xlang",         true,  "--xlang              Support for extra program constructs (imperative,...)"),
      LeonOptionDef("parse",         true,  "--parse              Checks only whether the program is valid PureScala"),
      LeonOptionDef("debug",         false, "--debug=[1-5]        Debug level"),
      LeonOptionDef("help",          true,  "--help               This help")
    )

  def displayHelp() {
    println("usage: leon [--xlang] [--help] [--synthesis] [--help] [--debug=<N>] [..] <files>")
    println
    println("Leon options are:")
    for (opt <- allOptions.toSeq.sortBy(_.name)) {
      println("   "+opt.description)
    }
    sys.exit(1)
  }

  def processOptions(reporter: Reporter, args: List[String]) = {
    val phases = allPhases

    val allOptions = this.allOptions

    val allOptionsMap = allOptions.map(o => o.name -> o).toMap

    // Detect unknown options:
    val options = args.filter(_.startsWith("--"))

    val leonOptions = options.flatMap { opt =>
      val leonOpt: LeonOption = opt.substring(2, opt.length).split("=", 2).toList match {
        case List(name, value) =>
          LeonValueOption(name, value)
        case List(name) => name
          LeonFlagOption(name)
        case _ =>
          reporter.fatalError("Woot?")
      }

      if (allOptionsMap contains leonOpt.name) {
        (allOptionsMap(leonOpt.name).isFlag, leonOpt) match {
          case (true,  LeonFlagOption(name)) =>
            Some(leonOpt)
          case (false, LeonValueOption(name, value)) =>
            Some(leonOpt)
          case _ =>
            System.err.println("Invalid option usage: "+opt)
            displayHelp()
            None
        }
      } else {
        System.err.println("leon: '"+opt+"' is not a valid option. See 'leon --help'")
        None
      }
    }

    var settings  = Settings()

    // Process options we understand:
    for(opt <- leonOptions) opt match {
      case LeonFlagOption("synthesis") =>
        settings = settings.copy(synthesis = true, xlang = false, analyze = false)
      case LeonFlagOption("xlang") =>
        settings = settings.copy(synthesis = false, xlang = true)
      case LeonFlagOption("parse") =>
        settings = settings.copy(synthesis = false, xlang = false, analyze = false)
      case LeonFlagOption("help") =>
        displayHelp()
      case _ =>
    }

    LeonContext(settings = settings, reporter = reporter)
  }

  implicit def phaseToPipeline[F, T](phase: LeonPhase[F, T]): Pipeline[F, T] = new PipeCons(phase, new PipeNil())

  def computePipeLine(settings: Settings): Pipeline[List[String], Unit] = {
    import purescala.Definitions.Program

    val pipeBegin = phaseToPipeline(plugin.ExtractionPhase)

    val pipeTransforms: Pipeline[Program, Program] =
      if (settings.xlang) {
        ArrayTransformation andThen
        EpsilonElimination andThen
        ImperativeCodeElimination andThen
        FunctionClosure
      } else {
        NoopPhase[Program]()
      }

    val pipeSynthesis: Pipeline[Program, Program] =
      if (settings.synthesis) {
        synthesis.SynthesisPhase
      } else {
        NoopPhase[Program]()
      }

    val pipeAnalysis: Pipeline[Program, Program] =
      if (settings.analyze) {
        AnalysisPhase
      } else {
        NoopPhase[Program]()
      }

    pipeBegin followedBy
    pipeTransforms followedBy
    pipeSynthesis followedBy
    pipeAnalysis andThen
    ExitPhase()
  }

  def main(args : Array[String]) : Unit = {
    val reporter = new DefaultReporter()

    // Process options
    val ctx = processOptions(reporter, args.toList)

    // Compute leon pipeline
    val pipeline = computePipeLine(ctx.settings)

    // Run phases
    pipeline.run(ctx)(args.toList)
  }
}

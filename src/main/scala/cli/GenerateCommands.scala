package cli
import java.nio.file.Path

import assembly.cloning.RestrictionEnzymes
import assembly.synthesis._
import better.files._
import cats.implicits._
import com.monovore.decline._
import pwms.{LoaderPWM, PWM}

import scala.util.{Success, Try}

trait GenerateCommands extends ConsensusCommands with InsertCommands {


  def makeParameters(sequence: String, maximumRepeatSize: Int = 20, enzymes: List[String]) = {

    val template = new StringTemplate(sequence)

    val avoid =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => enzymes.contains(e)})
    GenerationParameters(template, maximumRepeatSize, ContentGC.default, avoid)
  }

  val avoid_enzymes = Opts.options[String](long = "avoid", short = "a", help = "Avoid enzymes")

  val max_repeat = Opts.option[Int](long = "max_repeats", short = "rep", help = "Maximum repeat length").withDefault(19)

  val gc_min = Opts.option[Double](long = "gc_min", short = "gmin", help = "minimum GC content").withDefault(0.25)
  val gc_max = Opts.option[Double](long = "gc_max", short = "gmax", help = "maximum GC content").withDefault(0.7)


  protected lazy val synthesis = Command(
    name = "synthesize", header = "Check synthesize"
  ){
    (sequence, max_repeat, avoid_enzymes, gc_min, gc_max).mapN{ (s, rep,  avoid, g_min, g_max) =>
      val template = new StringTemplate(s)
      val restrictions =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid.toList.contains(e)})
      val params = GenerationParameters(template, rep, ContentGC.default.copy(minTotal = g_min, maxTotal = g_max), restrictions)
      println(s"repeats are ${if(params.checkRepeats(s)) "OK" else "NOT OK"}, longest possible repeat should be less than ${rep}")
      println(s"longest found repeats have length of ${params.findLongestRepeats(s).head._1.size} and are:\n")
      println(params.findLongestRepeats(s))
      println(s"GC is ${if(params.checkGC(s)) "OK" else "NOT OK"}, GC ratios is ${params.contentGC.ratioGC(s)} with optional between ${params.contentGC.minTotal} and ${params.contentGC.maxTotal}")
      if(params.checkEnzymes(s)) println(s"sequence is not cut by ${avoid.toList.mkString(",")}  that is OK") else println(s"sequence can be cut by ${avoid.toList.mkString(",")} that is NOT OK!1")
      println(s"overall sequence is ${if(params.check(s)) "OK" else "NOT OK"}")
    }
  }

  protected val synthesisSubcommand = Opts.subcommand(synthesis)

  val max_tries = Opts.option[Int](long = "tries", short = "t", help = "Maximum number of attempts to generate a good sequence").withDefault(3000)


  case class GenerationParametersPWM(template: PWM,
                       maxRepeatSize: Int,
                       contentGC: ContentGC,
                       enzymes: RestrictionEnzymes) extends GenerationParameters {
    override def checkEnzymes(sequence: String): Boolean = !enzymes.canCut(sequence, true) //temporal bug fix

    override def check(sequence: String): Boolean = checkEnzymes(sequence) && checkRepeats(sequence) && checkGC(sequence) && !sequence.contains("-")

  }

//filePWM
  protected lazy val generate: Command[Unit] = Command(
    name = "generate", header = "Generates from PWM"
  ){
    (path,  delimiter, outputFile, verbose, max_tries, max_repeat, avoid_enzymes, gc_min, gc_max).mapN{ (p, d, o, v, tries, rep,  avoid, g_min, g_max) =>

      val avoidList = RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid.toList.contains(e)}
      println(s"From avoided enzymes (${avoid.toList.mkString(",")}) following enzymes where found: ${avoidList}")
      val restrictions =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid.toList.contains(e)})
      val f = o.toFile.toScala
      val fileMap: Map[String, PWM] = LoaderPWM.load(p.toFile.toScala, delimiter = d)
      def params(pwm: PWM)  =  GenerationParametersPWM(pwm, rep, ContentGC.default.copy(minTotal = g_min, maxTotal = g_max), restrictions)
      println(s"${fileMap.size} PWMs processed for path ${p.toFile.toScala.path}!")
      val fasta = fileMap.foldLeft(""){
        case (acc, (f, pwm)) =>
          println(s"preparing sequences for PWM ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
          val p = params(pwm)
          Try{ SequenceGenerator.randomize(p, tries) } match {
            case Success(str) =>
              val stat = s""">${f} GC = ${p.contentGC.ratioGC(str)}, longest repeat = ${p.findLongestRepeats(str).head._1.size}"""
              println(s"successfully generate good to synthesize sequence for ${stat}")
              val name = if(v) stat else ">" + File(f).nameWithoutExtension
              acc + name + "\n" + str + "\n"
            case scala.util.Failure(exception) =>
              println(s"could not generate good to synthesize sequence for ${f} with ${tries} attempts")
              acc
          }
      }
      println(s"writing results as FASTA to ${f.path}")
      f.overwrite(fasta)
    }
  }
  val generateSubcommand = Opts.subcommand(generate)

}

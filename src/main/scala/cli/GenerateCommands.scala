package cli
import java.nio.file.Path

import assembly.cloning.{GoldenGate, RestrictionEnzyme, RestrictionEnzymes}
import assembly.synthesis._
import better.files._
import cats.data.NonEmptyList
import cats.implicits._
import com.monovore.decline._
import pwms.{LoaderPWM, PWM}
import assembly.extensions._

import scala.util.{Success, Try}

trait GenerateCommands extends ConsensusCommands with InsertCommands with CloningCommands {


  def makeParameters(sequence: String, maximumRepeatSize: Int = 20, enzymes: List[String]): GenerationParameters = {

    val template = new StringTemplate(sequence)

    val avoid =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => enzymes.contains(e)})
    GenerationParameters(template, maximumRepeatSize, ContentGC.default, avoid)
  }

  val avoid_enzymes: Opts[NonEmptyList[String]] = Opts.options[String](long = "avoid", short = "a", help = "Avoid enzymes").withDefault(NonEmptyList("BsmBI", List("BsaI")))

  val max_repeat: Opts[Int] = Opts.option[Int](long = "max_repeats", short = "r", help = "Maximum repeat length").withDefault(19)

  val instances = Opts.option[Int](long = "instances", short = "i", help = "Maximum number of instances per template").withDefault(1)


  val gc_min = Opts.option[Double](long = "gc_min", short = "", help = "minimum GC content").withDefault(0.25)
  val gc_max = Opts.option[Double](long = "gc_max", short = "", help = "maximum GC content").withDefault(0.7)


  protected lazy val synthesis = Command(
    name = "synthesize", header = "Check synthesize"
  ){
    (sequence, max_repeat, avoid_enzymes, gc_min, gc_max, instances).mapN{ (s, rep,  avoid, g_min, g_max, inst) =>
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

  val max_tries = Opts.option[Int](long = "tries", short = "t", help = "Maximum number of attempts to generate a good sequence").withDefault(10000)

  val template_repeats = Opts.option[Int](long = "tries", short = "t", help = "Maximum number of attempts to generate a good sequence").withDefault(10000)

  case class GenerationParametersPWM(template: PWM,
                       maxRepeatSize: Int,
                       contentGC: ContentGC,
                       enzymes: RestrictionEnzymes) extends GenerationParameters {


    override def check(sequence: String): Boolean = checkEnzymes(sequence) && checkRepeats(sequence) && checkGC(sequence) && !sequence.contains("-")

  }

  def generateSequences(path: Path, delimiter: String, outputFile: Path,
                        verbose: Boolean, max_tries: Int, max_repeat: Int,
                        avoid_enzymes: NonEmptyList[String], gc_min: Double, gc_max: Double, number: Int, enzyme: String, stickyLeft: String, stickyRight: String) = {
    val avoidList = RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)}
    println(s"From avoided enzymes (${avoid_enzymes.toList.mkString(",")}) following enzymes where found: ${avoidList}")
    val restrictions =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)})
    val fl = outputFile.toFile.toScala
    val fileMap: Map[String, PWM] = LoaderPWM.load(path.toFile.toScala, delimiter = delimiter)
    def params(pwm: PWM)  =  GenerationParametersPWM(pwm, max_repeat, ContentGC.default.copy(minTotal = gc_min, maxTotal = gc_max), restrictions)
    println(s"${fileMap.size} PWMs processed for path ${path.toFile.toScala.path}!")
    val fasta = fileMap.foldLeft(""){
      case (acc, (f, pwm)) =>
        println(s"preparing sequences for PWM ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
        val p = params(pwm)

        val gold = SequenceGeneratorGold(GoldenGate(enzymeFromName(enzyme)))
        Try{
          if(stickyLeft =="" || stickyRight =="") gold.randomizeMany(p, number, max_tries) else gold.generateMany(p, number, max_tries, stickyLeft, stickyRight)
        } match {
          case Success(results) =>
            val elements: Seq[String] = results.map {
              str =>
                val stat = s""">${f} GC = ${p.contentGC.ratioGC(str)}, longest repeat = ${p.findLongestRepeats(str).head._1.size}"""
                println(s"successfully generated good to synthesize sequence for ${stat}")
                val name = if (verbose) stat else ">" + File(f).nameWithoutExtension.take(29)
                name + "\n" + str + "\n"
            }
            acc ++ elements.mkString("")

          case scala.util.Failure(exception) =>
            println(s"could not generate good to synthesize sequence for ${f} with ${max_tries} attempts")
            acc
        }
    }
    println(s"writing results as FASTA to ${fl.path}")
    fl.overwrite(fasta)
  }

  protected lazy val cloning = Opts.option[String](long = "enzyme", short = "e", help = "Golden gate enzyme for cloning, if nothing is chosen no GoldenGate sites are added").withDefault("")
  protected lazy val sticky_left = Opts.option[String](long = "sticky_left", short = "", help = "Flank assembled sequence from the left with sticky side").withDefault("")
  protected lazy val sticky_right = Opts.option[String](long = "sticky_right", short = "", help = "Flank assembled sequence from the right with sticky side").withDefault("")


  //filePWM
  protected lazy val generate: Command[Unit] = Command(
    name = "generate", header = "Generates from PWM"
  ){
    (path,  delimiter, outputFile, verbose, max_tries, max_repeat, avoid_enzymes, gc_min, gc_max, instances, cloning, sticky_left, sticky_right).mapN(generateSequences)
  }
  val generateSubcommand = Opts.subcommand(generate)


}

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
import cats.data.NonEmptyList
import cats.data._

import scala.util.{Success, Try}

/**
  * NOTE extremely buggy right now!
  */
trait AdvancedGenerateCommands extends GenerateCommands {

  val max_repeats_arrays: Opts[NonEmptyList[Int]] = Opts.options[Int](long = "max_repeats", short = "r", help = "Segments with cumulative maximum repeats")
  val retries: Opts[Int] = Opts.option[Int](long = "retries", help = "Retry whole generation").withDefault(10)


  def generateRepeatedSequences(path: Path, delimiter: String, outputFile: Path, max_repeats_arrays: NonEmptyList[Int],
                                verbose: Boolean,
                                max_tries: Int,
                                retries: Int,
                                avoid_enzymes: NonEmptyList[String],
                                gc_min: Double, gc_max: Double,
                                value: Double,
                                enzyme: String, stickyLeft: String, stickyRight: String,
                                win_gc_size1: Int, win_gc_min1: Double, win_gc_max1: Double,
                                win_gc_size2: Int, win_gc_min2: Double, win_gc_max2: Double
                       ): Unit = {
    val avoidList = RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)}
    println(s"From avoided enzymes (${avoid_enzymes.toList.mkString(",")}) following enzymes where found: ${avoidList}")
    val restrictions =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)})
    val fl = outputFile.toFile.toScala
    val fileMap: Map[String, PWM] = LoaderPWM.load(path.toFile.toScala, delimiter = delimiter)
    val contentGC = ContentGC(gc_min, gc_max, List(WindowGC(win_gc_size1, win_gc_min1, win_gc_max1),WindowGC(win_gc_size2, win_gc_min2, win_gc_max2)).filter(_.size > 0))
    //val maxRepeatsArray =
    println(s"${fileMap.size} PWMs processed for path ${path.toFile.toScala.path}!")
    fl.clear()
    val initial_max_repeat = max_repeats_arrays.head
    for{
      (f, pwm) <- fileMap
    } {
      println(s"preparing sequences for PWM ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
      val p =  GenerationParametersPWM(pwm, initial_max_repeat, contentGC, restrictions) // Set(stickyLeft, stickyRight))
      //val generator = SequenceGenerator(GoldenGate(enzymeFromName(enzyme), "N"), sticky_diff, sticky_gc, maxStickyTries) //TODO sticky stuff
      tryRepeatedGenerate(retries)(max_tries, value)(p, max_repeats_arrays) match {
          case scala.util.Success(str) =>
              val stat = s""">${f} GC = ${p.contentGC.ratioGC(str)}, longest repeat = ${p.findLongestRepeats(str).head._1.size} length = ${str.length}"""
              println(s"successfully generated good to synthesize sequence for ${stat}")
              val name = if (verbose) stat else ">" + File(f).nameWithoutExtension.take(29)
              fl.append(name + "\n" + str + "\n")
              println(s"FINISHED writing FASTA to ${fl.path}")

          case scala.util.Failure(exception) =>
            println(s"could not generate good to synthesize sequence for ${f} with ${max_tries} attempts")
            println(s"FAILED generating sequence for ${fl.path}")
      }
    }
  }

  @scala.annotation.tailrec
  private def tryRepeatedGenerate(retries: Int)(max_tries: Int, value: Double)
                              (p: GenerationParametersPWM, repeatedArray: NonEmptyList[Int]): Try[String] = {
    Try(repeatedGenerate(max_tries, value)(p, repeatedArray)) match {
      case s: scala.util.Success[String] => s
      case f: scala.util.Failure[String] => if(retries <= 0) f else tryRepeatedGenerate(retries-1)(max_tries, value)(p, repeatedArray)
    }
  }

  @scala.annotation.tailrec
  private def repeatedGenerate(max_tries: Int, value: Double)
                              (p: GenerationParametersPWM, repeatedArray: NonEmptyList[Int]): String = repeatedArray match {
    case NonEmptyList(head, Nil) =>
      SequenceGenerator.randomize(p.copy(maxRepeatSize = head), max_tries)

    case NonEmptyList(head, tail) =>
      val str = SequenceGenerator.randomize(p.copy(maxRepeatSize = head), max_tries)
      repeatedGenerate(max_tries, value)(p.copy(template = p.template.concatAfter(str, value)), NonEmptyList.fromListUnsafe(tail))
  }

  /**
  def generateRepeatedSequences(path: Path, delimiter: String, outputFile: Path, max_repeats_arrays: List[Int],
                                verbose: Boolean,
                                max_tries: Int,
                                avoid_enzymes: NonEmptyList[String],
                                gc_min: Double, gc_max: Double,
                                value: Double,
                                enzyme: String, stickyLeft: String, stickyRight: String,
                                win_gc_size1: Int, win_gc_min1: Double, win_gc_max1: Double,
                                win_gc_size2: Int, win_gc_min2: Double, win_gc_max2: Double
    */

  //filePWM
  protected lazy val repeatedGenerate: Command[Unit] = Command(
    name = "repeated_generate", header = "Generates repeated PWM with repeats thresholds"
  ){
    (path,  delimiter, outputFile, max_repeats_arrays, verbose,
      max_tries, retries, avoid_enzymes,
      gc_min, gc_max,
      value, cloning,
      sticky_left, sticky_right,
      win_gc_size1, win_gc_min1, win_gc_max1,  win_gc_size2, win_gc_min2, win_gc_max2
    ).mapN(generateRepeatedSequences)
  }

  val log_repeats: Opts[Int] = Opts.option[Int](long = "log_repeats", help = "if more than 0 then it writes repeats which is more frequent than N to the log file with their coordinates")
    .withDefault(0)

  def generateAnalytics(path: Path, delimiter: String, outputFile: Path,
                        verbose: Boolean,
                        max_tries: Int, max_repeat: Int, log_repeats: Int,
                        avoid_enzymes: NonEmptyList[String], gc_min: Double, gc_max: Double,
                        win_gc_size1: Int, win_gc_min1: Double, win_gc_max1: Double,
                        win_gc_size2: Int, win_gc_min2: Double, win_gc_max2: Double,
                        sticky_diff: Int, sticky_gc: Int, maxStickyTries: Int
                       ): Unit = {
    val avoidList = RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)}
    println(s"From avoided enzymes (${avoid_enzymes.toList.mkString(",")}) following enzymes where found: ${avoidList}")
    val restrictions =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)})
    val fileMap: Map[String, PWM] = LoaderPWM.load(path.toFile.toScala, delimiter = delimiter)
    val contentGC = ContentGC(gc_min, gc_max, List(WindowGC(win_gc_size1, win_gc_min1, win_gc_max1),WindowGC(win_gc_size2, win_gc_min2, win_gc_max2)).filter(_.size > 0))
    println(s"${fileMap.size} PWMs processed for path ${path.toFile.toScala.path}!")
    val fl = outputFile.toFile.toScala
    fl.clear()
    for{
      (f, pwm) <- fileMap
    } {
      println(s"preparing sequences for PWM ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
      //val gold = SequenceGeneratorGold(GoldenGate(enzymeFromName(enzyme), "N"), sticky_diff, sticky_gc, maxStickyTries)
      val analytics =  new GenerationParametersAnalytics(pwm, max_repeat, contentGC, restrictions, log_repeats = log_repeats)

      SequenceGenerator.tryRandomize(analytics, max_tries) match {
        case Success(str) =>
            val stat = s""">${f} GC = ${analytics.contentGC.ratioGC(str)}, longest repeat = ${analytics.findLongestRepeats(str).head._1.size}"""
            println(s"successfully generated good to synthesize sequence for ${stat}")
            val name = if (verbose) stat else ">" + File(f).nameWithoutExtension.take(29)
            fl.append(name + "\n" + str + "\n")
            println(s"finished writing FASTA to ${fl.path}")
            val info = File(fl.path.getParent) / (s"SUCCEEDED_PWM_max_repeats_${max_repeat}_" + name + ".tsv")
            analytics.repeatsPWM.write(info)
            println("wrote analytics: "+ info.pathAsString)
            if(log_repeats>0){
              val log = File(fl.path.getParent) / (s"SUCCEEDED_REPEATS_LOG_max_repeats_${max_repeat}_" + name + ".txt")
              val lg = analytics.getRepeatStats().toList.sortBy( - _._2.length).map{ case (rep, list) => rep + list.mkString(" : ", ", ", "")}.mkString("\n")
              log.write(analytics.getPrintedRepeatStats())
              println(s"wrote repeats log: ${log.pathAsString}")
            }




        case scala.util.Failure(exception) =>
          println(s"could not generate good to synthesize sequence for ${f} with ${max_tries} attempts")

          val name = ">" + File(f).nameWithoutExtension.take(29)
          val info = File(fl.path.getParent) / (s"FAILED__PWM_max_repeats_${max_repeat}_" + name + ".tsv")
          analytics.repeatsPWM.write(info)
          println("wrote analytics: "+ info.pathAsString)
          if(log_repeats>0){
            val log = File(fl.path.getParent) / (s"FAILED_REPEATS_LOG_max_repeats_${max_repeat}_" + name + ".txt")
            log.write(analytics.getPrintedRepeatStats())
            println(s"wrote repeats log: ${log.pathAsString}")

          }
      }
    }
  }

  /**
    * def generateAnalytics(path: Path, delimiter: String, outputFile: Path,
    * verbose: Boolean,
    * max_tries: Int, max_repeat: Int,
    * avoid_enzymes: NonEmptyList[String], gc_min: Double, gc_max: Double,
    * enzyme: String,
    * win_gc_size1: Int, win_gc_min1: Double, win_gc_max1: Double,
    * win_gc_size2: Int, win_gc_min2: Double, win_gc_max2: Double,
    * sticky_diff: Int, sticky_gc: Int, maxStickyTries: Int
    * )
    */

  protected lazy val generate_analytical: Command[Unit] = Command(
    name = "generate_analytical", header = "Generates from PWM with information about repeats"
  ){
    (path,  delimiter, outputFile, verbose,
      max_tries, max_repeat, log_repeats,
      avoid_enzymes, gc_min, gc_max,
      win_gc_size1, win_gc_min1, win_gc_max1,  win_gc_size2, win_gc_min2, win_gc_max2,
      sticky_diff, sticky_gc, sticky_tries).mapN(generateAnalytics)
  }

  val advancedGenerateSubcommand: Opts[Unit] = Opts.subcommand(repeatedGenerate).orElse(Opts.subcommand(generate_analytical))


}

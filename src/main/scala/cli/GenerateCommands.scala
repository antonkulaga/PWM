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
import breeze.linalg.DenseMatrix

import scala.collection.mutable
import scala.util.{Success, Try}


trait GenerateCommands extends ConsensusCommands with InsertCommands with CloningCommands {

  def makeParameters(sequence: String, maximumRepeatSize: Int = 20, enzymes: List[String]): GenerationParameters = {

    val template = new StringTemplate(sequence)

    val avoid =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => enzymes.contains(e)})
    GenerationParameters(template, maximumRepeatSize, ContentGC.default, avoid)
  }

  val avoid_enzymes: Opts[NonEmptyList[String]] = Opts.options[String](long = "avoid", short = "a", help = "Avoid enzymes").withDefault(NonEmptyList("BsmBI", List("BsaI")))

  val max_repeat: Opts[Int] = Opts.option[Int](long = "max_repeats", short = "r", help = "Maximum repeat length").withDefault(19)

  val instances: Opts[Int] = Opts.option[Int](long = "instances", short = "i", help = "Maximum number of instances per template").withDefault(1)

  val gc_min: Opts[Double] = Opts.option[Double](long = "gc_min", short = "", help = "minimum GC content").withDefault(0.25)
  val gc_max: Opts[Double] = Opts.option[Double](long = "gc_max", short = "", help = "maximum GC content").withDefault(0.7)
  val win_gc_size1: Opts[Int] = Opts.option[Int](long = "win_gc_size1", short = "", help = "GC window1 size, if < 1 then window is not used").withDefault(100)
  val win_gc_min1: Opts[Double] = Opts.option[Double](long = "win_gc_min1", short = "", help = "window1 minimum GC content").withDefault(0.25)
  val win_gc_max1: Opts[Double] = Opts.option[Double](long = "win_gc_max1", short = "", help = "window1 maximum GC content").withDefault(0.75)
  val win_gc_size2: Opts[Int] = Opts.option[Int](long = "win_gc_size2", short = "", help = "GC window2 size, if < 1 then window is not used").withDefault(50)
  val win_gc_min2: Opts[Double] = Opts.option[Double](long = "win_gc_min2", short = "", help = "window2 minimum GC content").withDefault(0.15)
  val win_gc_max2: Opts[Double] = Opts.option[Double](long = "win_gc_max2", short = "", help = "window2 maximum GC content").withDefault(0.8)

  val sticky_diff: Opts[Int] = Opts.option[Int](long = "sticky_diff", short = "", help = "minimal difference between sticky sides").withDefault(1)
  val sticky_gc: Opts[Int] = Opts.option[Int](long = "sticky_gc", short = "", help = "minimal numbers of G || C nucleotides in the sticky end").withDefault(1)


  def synthesize(s: String, rep: Int,  avoid: NonEmptyList[String],
                 g_min: Double, g_max: Double,
                 win_gc_size1: Int, win_gc_min1: Double, win_gc_max1: Double,
                 win_gc_size2: Int, win_gc_min2: Double, win_gc_max2: Double): Unit ={
    val template = new StringTemplate(s)
    val restrictions =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid.toList.contains(e)})
    val contentGC = ContentGC(g_min, g_max, List(WindowGC(win_gc_size1, win_gc_min1, win_gc_max1),WindowGC(win_gc_size2, win_gc_min2, win_gc_max2)).filter(_.size > 0))

    val params = GenerationParameters(template, rep, contentGC, restrictions)
    println(s"repeats are ${if(params.checkRepeats(s)) "OK" else "NOT OK"}, longest possible repeat should be less than ${rep}")
    println(s"longest found repeats have length of ${params.findLongestRepeats(s).head._1.size} and are:\n")
    println(params.findLongestRepeats(s))
    println(s"GC is ${if(params.checkGC(s)) "OK" else "NOT OK"}, GC ratios is ${params.contentGC.ratioGC(s)} with optional between ${params.contentGC.minTotal} and ${params.contentGC.maxTotal}")
    if(params.checkEnzymes(s)) println(s"sequence is not cut by ${avoid.toList.mkString(",")}  that is OK") else println(s"sequence can be cut by ${avoid.toList.mkString(",")} that is NOT OK!1")
    println(s"overall sequence is ${if(params.check(s)) "OK" else "NOT OK"}")
  }

  protected lazy val synthesis = Command(
    name = "synthesize", header = "Check synthesize"
  ){
    (sequence, max_repeat, avoid_enzymes,
      gc_min, gc_max,
      win_gc_size1, win_gc_min1, win_gc_max1,  win_gc_size2, win_gc_min2, win_gc_max2
    ).mapN(synthesize)
  }

  protected val synthesisSubcommand = Opts.subcommand(synthesis)

  val max_tries: Opts[Int] = Opts.option[Int](long = "tries", short = "t", help = "Maximum number of attempts to generate a good sequence").withDefault(10000)
  val sticky_tries: Opts[Int] = Opts.option[Int](long = "sticky_tries", short = "", help = "Maximum number of attempts to select golden-gate edges if all other parameters match").withDefault(256)


  //val template_repeats = Opts.option[Int](long = "tries", short = "t", help = "Maximum number of attempts to generate a good sequence").withDefault(10000)

  case class GenerationParametersPWM(template: PWM,
                                     maxRepeatSize: Int,
                                     contentGC: ContentGC,
                                     enzymes: RestrictionEnzymes,
                                     avoidSequences: Set[String] = Set.empty) extends GenerationParameters {

    //override def checkRepeats(sequence: String) = sequence.sliding(maxRepeatSize).toSet.nonEmpty

    override def check(sequence: String): Boolean = checkEnzymes(sequence) && checkRepeats(sequence) && checkGC(sequence) && !sequence.contains("-") && !avoidSequences.exists(sequence.contains)

    override def withReplacement(sequence: String, position: Int): GenerationParameters = copy(template = template.withSequenceReplacement(sequence, position))
  }

  class GenerationParametersAnalytics(val template: PWM,
                                      val maxRepeatSize: Int,
                                      val contentGC: ContentGC,
                                      val enzymes: RestrictionEnzymes,
                                      val avoidSequences: Set[String] = Set.empty,
                                      val log_repeats: Int = 0
                                          ) extends  GenerationParameters {

    val repeatsPWM: PWM = template.copy(matrix = DenseMatrix.zeros(template.matrix.rows, template.matrix.cols))
    protected val repeatStats: collection.mutable.Map[String, collection.immutable.List[Int]] = collection.mutable.Map.empty[String, collection.immutable.List[Int]]
    def getRepeatStats(): mutable.Map[String, collection.immutable.List[Int]] = if(log_repeats <=0) collection.mutable.Map.empty[String, collection.immutable.List[Int]] else repeatStats.filter(_._2.length >= log_repeats)
    def getPrintedRepeatStats() = getRepeatStats.toList.sortBy(- _._2.length).map{ case (rep, list) => rep + list.mkString(" : ", ", ", "")}.mkString("\n")

    override def check(sequence: String): Boolean = checkEnzymes(sequence) && checkRepeats(sequence) && checkGC(sequence) && !sequence.contains("-") && !avoidSequences.exists(sequence.contains)

    /**
      * UGLY mutable!!!
      * @param str
      * @param len
      * @return
      */
    override def checkRepeats(str: String, len: Int): Boolean = {
      val mp = scala.collection.mutable.Map[String, List[Int]]()
      for(i <- 0 to str.length - len) {
        val s = str.slice(i, i + len)
        if(mp.contains(s)){
          mp.update(s, i::mp(s))
        } else mp.update(s, List(i))
      }
      for{
        (s, list) <- mp
        if list.length > 1
        (n, j) <- s.zipWithIndex
        i <- list
        pos = j + i
      } {
        val r = repeatsPWM.indexes(n.toString)
        val v = repeatsPWM.matrix(r, pos)
        repeatsPWM.matrix.update(r, pos, v+1)
      }
      if(log_repeats > 0){
        for{
          (s, list) <- mp
          if list.length > log_repeats
        }
          {
            if(repeatStats.contains(s)) repeatStats.update(s, repeatStats(s) ++ list) else repeatStats.update(s, list)
          }
      }
      val res = !mp.values.exists(_.length > 1)
      mp.clear()
      res
    }

    override def withReplacement(sequence: String, position: Int): GenerationParameters = ??? //copy(template = template.withSequenceReplacement(sequence, position))

  }


  /**
    * generates the sequece according to parameters
    * @param path Path to PWM
    * @param delimiter delimiter used in csv/tsv file
    * @param outputFile output sequence
    * @param verbose tell more debug information
    * @param max_tries maximum tried that one can do for generation
    * @param max_repeat
    * @param avoid_enzymes
    * @param gc_min
    * @param gc_max
    * @param number
    * @param enzyme
    * @param stickyLeft
    * @param stickyRight
    * @param win_gc_size1
    * @param win_gc_min1
    * @param win_gc_max1
    * @param win_gc_size2
    * @param win_gc_min2
    * @param win_gc_max2
    * @param sticky_diff
    * @param sticky_gc
    * @param maxStickyTries
    */
  def generateSequences(path: Path, delimiter: String, outputFile: Path,
                        verbose: Boolean,
                        max_tries: Int, max_repeat: Int,
                        avoid_enzymes: NonEmptyList[String], gc_min: Double, gc_max: Double,
                        number: Int, enzyme: String, stickyLeft: String, stickyRight: String,
                        win_gc_size1: Int, win_gc_min1: Double, win_gc_max1: Double,
                        win_gc_size2: Int, win_gc_min2: Double, win_gc_max2: Double,
                        sticky_diff: Int, sticky_gc: Int, maxStickyTries: Int
                       ): Unit = {
    val avoidList = RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)}
    println(s"From avoided enzymes (${avoid_enzymes.toList.mkString(",")}) following enzymes where found: ${avoidList}")
    val restrictions =  RestrictionEnzymes(RestrictionEnzymes.commonEnzymesSet.filter{ case (e, _) => avoid_enzymes.toList.contains(e)})
    val fl = outputFile.toFile.toScala
    val fileMap: Map[String, PWM] = LoaderPWM.load(path.toFile.toScala, delimiter = delimiter)
    val contentGC = ContentGC(gc_min, gc_max, List(WindowGC(win_gc_size1, win_gc_min1, win_gc_max1),WindowGC(win_gc_size2, win_gc_min2, win_gc_max2)).filter(_.size > 0))
    def params(pwm: PWM)  =  GenerationParametersPWM(pwm, max_repeat, contentGC, restrictions)
    println(s"${fileMap.size} PWMs processed for path ${path.toFile.toScala.path}!")
    fl.clear()
    for{
      (f, pwm) <- fileMap
    } {
      println(s"preparing sequences for PWM ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
      val p = params(pwm)
      val gold = SequenceGeneratorGold(GoldenGate(enzymeFromName(enzyme), "N"), sticky_diff, sticky_gc, maxStickyTries)
      Try {
        if (stickyLeft == "" || stickyRight == "") gold.randomizeMany(p, number, max_tries) else gold.generateMany(p, number, max_tries, stickyLeft, stickyRight)
      } match {
        case Success(results) =>
          for(str <- results) {
              val stat = s""">${f} GC = ${p.contentGC.ratioGC(str)}, longest repeat = ${p.findLongestRepeats(str).head._1.size}"""
              println(s"successfully generated good to synthesize sequence for ${stat}")
              val name = if (verbose) stat else ">" + File(f).nameWithoutExtension.take(29)
              fl.append(name + "\n" + str + "\n")
          }
          println(s"finished writing FASTA to ${fl.path}")

        case scala.util.Failure(exception) =>
          println(s"could not generate good to synthesize sequence for ${f} with ${max_tries} attempts")
      }
    }
  }

  protected lazy val cloning: Opts[String] = Opts.option[String](long = "enzyme", short = "e", help = "Golden gate enzyme for cloning, if nothing is chosen no GoldenGate sites are added").withDefault("")
  protected lazy val sticky_left: Opts[String] = Opts.option[String](long = "sticky_left", short = "", help = "Flank assembled sequence from the left with sticky side").withDefault("")
  protected lazy val sticky_right: Opts[String] = Opts.option[String](long = "sticky_right", short = "", help = "Flank assembled sequence from the right with sticky side").withDefault("")

  //filePWM
  protected lazy val generate: Command[Unit] = Command(
    name = "generate", header = "Generates from PWM"
  ){
    (path,  delimiter, outputFile, verbose,
      max_tries, max_repeat, avoid_enzymes,
      gc_min, gc_max,
      instances, cloning,
      sticky_left, sticky_right,
      win_gc_size1, win_gc_min1, win_gc_max1,  win_gc_size2, win_gc_min2, win_gc_max2,
      sticky_diff, sticky_gc, sticky_tries).mapN(generateSequences)
  }
  val generateSubcommand: Opts[Unit] = Opts.subcommand(generate)


}

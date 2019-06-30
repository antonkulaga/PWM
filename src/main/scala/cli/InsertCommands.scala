package cli

import java.nio.file.Path

import better.files._
import breeze.linalg.DenseMatrix
import cats.data.Validated
import cats.implicits._
import com.monovore.decline._
import pwms.{LoaderPWM, PWM}

import scala.util.Try

/**
  * Commands to insert the sequence into the PWM in the optimal places
  */
trait InsertCommands extends MergeCommands{


  protected val sequence = Opts.argument[String]("sequence")

  protected val positions = Opts.options[Int](long = "position", "positions to which insert the sequence", "p")
  protected val value = Opts.option[Double](long = "value", help = "default value for inserted nucleotides in PWM", short = "val").withDefault(100000.0)

  protected lazy val fix: Opts[List[(Int, Int)]] =
    Opts.option[String](long = "fix", short = "", help="Avoids insertions into positions specified as from-to;from-to;from-to")
      .mapValidated { string =>
        val strs: Array[String] = string.split(";")
        val list: List[Array[String]] = strs.map(v => v.split("-", 2)).toList
        if(list.forall{
          case Array(one, two) =>
            Try( (one.toInt , two.toInt)).isSuccess
          case _ => false
        })
          Validated.valid(list.map{ case Array(one, two) => (one.toInt, two.toInt)})
        else
          Validated.invalidNel(s"Cannot parse fixed spans $string")
      }
      .withDefault(List.empty[(Int, Int)])


  /**
    * Inserts the sequence manually into some positions inside of the PWM
    */
  protected lazy val manualInsertCommand = Command(
    name = "insert_at",
    header = "insert sequence manually into some positions"
  ) {
    (sequence, path, outputFile, positions, value, verbose, delimiter).mapN { (seq, f, o, ps, v, verb, d) =>
      val (where, pwm) = LoaderPWM.load(f, delimiter = d).head
      val u = pwm.withReplacement(seq, v, ps.toList:_*)
      info(s"reading PWM from ${where}")
      u.write(o)
      info(s"PWM with insertion written to ${o.toFile.toScala.path}")
      if(verb) {
        info(s"values after insertion")
        info(u.toString)
      }
    }
  }

  protected val manualInsertSubcommand: Opts[Unit] = Opts.subcommand(manualInsertCommand)

  protected lazy val outputFolder = Opts.argument[Path]("output folder or file")


  def insertStringIntoFile(sequence: String, number: Int, distance: Int, value: Double, verbose: Boolean, output: File, f: String, pwm: PWM, begin: Int, end: Int, fix: List[(Int, Int)] = Nil): File = {
    info(s"PWM found at ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
    info(s"inserting ${sequence} ${number} times with distance ${distance} and value ${value} inside PWM")
    val sm: DenseMatrix[Double] = pwm.sequenceToMatrix(sequence, value)
    val cand: Seq[(Int, Double)] = pwm.candidates(sm, distance, begin, end, fix)
    processCandidates(sm, number, value, verbose, output, f, pwm, cand)
  }

  protected def processCandidates(sm: DenseMatrix[Double], number: Int, value: Double, verbose: Boolean, output: File, f: String, pwm: PWM, cand: Seq[(Int, Double)]): File = {
    if (number > cand.size) {
      val e = s"Unfortunately got only ${cand.size} insertion positions instead of desired ${number}, inserting there"
      error(e)
    }
    val ns = Math.min(number, cand.size)
    val positions: Seq[Int] = cand.take(ns).map(_._1)
    val name = s"${sequence}_x${ns}_at_${positions.mkString("_")}_${File(f).nameWithoutExtension}.tsv"
    val upd = pwm.withReplacement(sm, value, positions: _*)
    val fl = if (output.isRegularFile) output else output / name
    info(s"writing insertion to ${fl.path} with length ${upd.matrix.cols} and mean coverage ${upd.meanCol}")
    upd.write(fl, overwrite = true)
    if (verbose) {
      info("new values are:")
      info(upd.toString)
    }
    fl
  }

  def insertFromFolder(sequence: String, path: Path, outputFolder: Path, number: Int, distance: Int, value: Double,
                       miss_score: Double, gapMultiplier: Double, verbose: Boolean, delimiter: String, begin: Int = 0, end: Int = Int.MaxValue, fix: List[(Int, Int)] = Nil): Unit = {
    val fileMap: Map[String, PWM] = LoaderPWM.load(path.toFile.toScala, miss_score, gapMultiplier, delimiter)
    fileMap.size match {
      case 0 => error("could not find any PWM files!")
      case 1 =>
        val (f, pwm) = fileMap.head
        info(s"only ${f} file is given as input")
        val output: File = outputFolder.toFile.toScala.createIfNotExists(false)
        insertStringIntoFile(sequence, number, distance, value, verbose, output, f, pwm, begin, end, fix)
      case _ =>
        val output: File = outputFolder.toFile.toScala.createIfNotExists(true)
        for((f, pwm) <-fileMap){
          insertStringIntoFile(sequence, number, distance, value, verbose, output, f, pwm, begin, end, fix)
        }
    }
  }

  val number = Opts.option[Int](long = "num", short = "n", help = "number of insertions").withDefault(1)
  val distance = Opts.option[Int](long = "distance", short = "dist", help = "minimum distance between two insertions if insertion number > 1").withDefault(0)
  val begin = Opts.option[Int](long = "begin", short = "b", help = "from which nucleotide to start insertions").withDefault(0)
  val end =  Opts.option[Int](long = "end", short = "e", help = "until which nucleotide to stop insertion").withDefault(Int.MaxValue)

  protected val insertCommand: Command[Unit] = Command(
    name = "insert",
    header = "inserts sequence into PWM into the best place"
  ) {

    (sequence, path, outputFolder, number, distance, value,  miss_score, gapMultiplier, verbose, delimiter, begin, end, fix).mapN(insertFromFolder)
  }

  protected val insertSubcommand: Opts[Unit] = Opts.subcommand(insertCommand)


  def insertPWMIntoFile(insert: PWM, number: Int, distance: Int, value: Double, verbose: Boolean, output: File, f: String, pwm: PWM, begin: Int, end: Int, fix: List[(Int, Int)]): File = {
    info(s"PWM found at ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
    info(s"inserting ${sequence} ${number} times with distance ${distance} and value ${value} inside PWM")
    assert(insert.indexes == pwm.indexes, "indexes of inserted PWM should be same as indexes of the target!")
    val cand: Seq[(Int, Double)] = pwm.candidates(insert.matrix, distance, begin, end, fix)
    processCandidates(insert.matrix, number, value, verbose, output, f, pwm, cand)
  }

  def insertPWMFromFolder(path_pwm: Path, path: Path, outputFolder: Path, number: Int, distance: Int, value: Double,
                       miss_score: Double, gapMultiplier: Double, verbose: Boolean, delimiter: String, begin: Int = 0, end: Int = Int.MaxValue, fix: List[(Int, Int)] = Nil): Unit = {
    val pwm: PWM = LoaderPWM.loadFile(path_pwm.toFile.toScala, miss_score, gapMultiplier, delimiter)
    val fileMap: Map[String, PWM] = LoaderPWM.load(path.toFile.toScala, miss_score, gapMultiplier, delimiter)
    fileMap.size match {
      case 0 => error("could not find any PWM files!")
      case 1 =>
        val (f, pwm) = fileMap.head
        info(s"only ${f} file is given as input")
        val output: File = outputFolder.toFile.toScala.createIfNotExists(false)
        insertPWMIntoFile(pwm, number, distance, value, verbose, output, f, pwm, begin, end, fix)
      case _ =>
        val output: File = outputFolder.toFile.toScala.createIfNotExists(true)
        for((f, pwm) <-fileMap){
          insertPWMIntoFile(pwm, number, distance, value, verbose, output, f, pwm, begin, end, fix)
        }
    }
  }


  protected lazy val path_pwm: Opts[Path] = Opts.argument[Path]("PWM file or folder to read from")

  protected val insertPWMCommand: Command[Unit] = Command(
    name = "insert_pwm",
    header = "inserts PWM into larger PWM into the best place"
  ) {
     (path_pwm, path, outputFolder, number, distance, value,  miss_score, gapMultiplier, verbose, delimiter, begin, end, fix).mapN(insertPWMFromFolder)
  }

  protected val insertPWMSubcommand: Opts[Unit] = Opts.subcommand(insertPWMCommand)


}
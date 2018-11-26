package cli

import java.nio.file.Path

import better.files._
import cats.implicits._
import com.monovore.decline._
import pwms.{LoaderPWM, PWM}


trait InsertCommands extends MergeCommands{


  protected val sequence = Opts.argument[String]("sequence")

  protected val positions = Opts.options[Int](long = "position", "positions to which insert the sequence", "p")
  protected val value = Opts.option[Int](long = "value", help = "default value for inserted nucleotides in PWM", short = "val").withDefault(100000)

  protected lazy val manualInsertCommand = Command(
    name = "insert_at",
    header = "insert sequence manually into some positions"
  ) {
    (sequence, path, outputFile, positions, value, verbose, delimiter).mapN { (seq, f, o, ps, v, verb, d) =>
      val (where, pwm) = LoaderPWM.load(f, delimiter = d).head
      val u = pwm.withInsertions(seq, v, ps.toList:_*)
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

  def insertIntoFile(sequence: String, number: Int, distance: Int, value: Double, verbose: Boolean, output: File, f: String, pwm: PWM): File = {
    info(s"PWM found at ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
    info(s"inserting ${sequence} ${number} times with distance ${distance} and value ${value} inside PWM")
    val cand = pwm.candidates(sequence, distance)
    if (number > cand.size) {
      val e = s"Unfortunately got only ${cand.size} insertion positions instead of desired ${number}, inserting there"
      error(e)
    }
    val ns = Math.min(number, cand.size)
    val positions: Seq[Int] = cand.take(ns).map(_._1)
    val name = s"${sequence}_x${ns}_at_${positions.mkString("_")}_${File(f).nameWithoutExtension}.tsv"
    val upd = pwm.withInsertions(sequence, value, positions: _*)
    val fl = if(output.isRegularFile) output else output / name
    info(s"writing insertion to ${fl.path} with length ${upd.matrix.cols} and mean coverage ${upd.meanCol}")
    upd.write(fl, overwrite = true)
    if (verbose) {
      info("new values are:")
      info(upd.toString)
    }
    fl
  }

  def insertFromFolder(sequence: String, path: Path, outputFolder: Path, number: Int, distance: Int, value: Double,
                       miss_score: Double, gapMultiplier: Double, verbose: Boolean, delimiter: String): Unit = {
    val fileMap: Map[String, PWM] = LoaderPWM.load(path.toFile.toScala, miss_score, gapMultiplier, delimiter)
    fileMap.size match {
      case 0 => error("could not find any PWM files!")
      case 1 =>
        val (f, pwm) = fileMap.head
        info(s"only ${f} file is given as input")
        val output: File = outputFolder.toFile.toScala.createIfNotExists(false)
        insertIntoFile(sequence, number, distance, value, verbose, output, f, pwm)
      case _ =>
        val output: File = outputFolder.toFile.toScala.createIfNotExists(true)
        for((f, pwm) <-fileMap){
          insertIntoFile(sequence, number, distance, value, verbose, output, f, pwm)
        }
    }

  }

  protected val insertCommand: Command[Unit] = Command(
    name = "insert",
    header = "inserts sequence into PWM into the best place"
  ) {
    val number = Opts.option[Int](long = "num", short = "n", help = "number of insertions").withDefault(1)
    val distance = Opts.option[Int](long = "distance", short = "dist", help = "minimum distance between two insertions if insertion number > 1").withDefault(0)

    (sequence, path, outputFolder, number, distance, value,  miss_score, gapMultiplier, verbose, delimiter).mapN { (seq, p, o, n, dist, v, miss, g, verb, d) =>
      insertFromFolder(seq, p, o, n , dist, v, miss, g, verb, d)
    }
  }

  protected val insertSubcommand: Opts[Unit] = Opts.subcommand(insertCommand)

}
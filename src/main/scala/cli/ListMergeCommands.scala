package cli


import java.nio.file.Path

import better.files._
import cats.implicits._
import com.monovore.decline._
import pwms.{LoaderPWM, PWM}
import wvlet.log.LogSupport

trait ListCommand extends LogSupport{

  protected lazy val path = Opts.argument[Path]("file or folder to read from")
  protected lazy val delimiter = Opts.option[String](long = "delimiter", short = "d", help = "delimiter to be used when parsing PWMs").withDefault("auto")
  protected lazy val verbose = Opts.flag("verbose", short="v", help="show values of the found PWMs").map(_=>true).withDefault(false)

  protected lazy val listCommand =Command(
    name = "list",
    header = "Lists known files"
  ) {
    (path, verbose, delimiter).mapN { (p, v, d) =>
      val fileMap: Map[String, PWM] = LoaderPWM.load(p.toFile.toScala, delimiter = d)
      println(s"${fileMap.size} PWMs processed for path ${p.toFile.toScala.path}!")
      for((f, pwm) <-fileMap){
        println(s"PWM found at ${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
        if(v){
          println("PWM values:")
          println(pwm)
          println(" \n")
        }
      }
    }
  }

  protected val listSubcommand =  Opts.subcommand(listCommand)
}

trait MergeCommands extends ListCommand{

  protected lazy val outputFile = Opts.argument[Path]("output file")
  protected lazy val gapMultiplier = Opts.option[Double](long = "gapmult", short = "g", help = "how much we care about gaps when inserting").withDefault(4.0)
  protected lazy val miss_score = Opts.option[Double](long = "miss", short = "m", help = "miss score (negative value required)").withDefault(-12.0)

  protected lazy val mergeCommand = Command(
    name = "merge",
    header = "merges all PWMs inside the folder into one"
  ) {
    (path, outputFile, verbose, delimiter).mapN { (p, o, v, d) =>
      val fileMap: Map[String, PWM] = LoaderPWM.load(p.toFile.toScala, delimiter = d)
      val output = o.toFile.toScala
      fileMap.size match {
        case 0 =>
          println("no PWMs found, nothing to merge!")
        case 1 =>
          val file = File(fileMap.keys.head)
          file.copyTo(output, true)
          println(s"only one file ${file.path} found and copied to ${output.path}")
        case more =>
          println("merging files:")
          for((f, pwm) <- fileMap) println(s"${f} with length ${pwm.matrix.cols} and mean coverage ${pwm.meanCol}")
          val merged: PWM = fileMap.values.reduce(_ |+| _)
          println(" \n")
          merged.write(o, overwrite = true)
          println(s"files merged and copied to ${output.path}")
          if(v) {
            println(s"merged PWM values are:")
            println(merged.toString)
          }
      }
    }
  }

  protected val mergeSubcommand =  Opts.subcommand(mergeCommand)
}
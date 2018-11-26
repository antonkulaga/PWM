package pwms

import java.nio.file.Path

import better.files._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._

object Main extends CommandApp(
  name = "PWM",
  header = "PWM inserter",
  main = {

    lazy val gapMultiplier = Opts.option[Double](long = "gapmult", short = "g", help = "how much we care about gaps when inserting").withDefault(4.0)
    lazy val mismatch = Opts.option[Double](long = "miss", short = "m", help = "mismatch score (negative value needed)").withDefault(-12.0)
    lazy val delimiter = Opts.option[String](long = "delimiter", short = "d", help = "delimiter to be used when parsing PWMs").withDefault("auto")
    lazy val path = Opts.argument[Path]("file or folder to read from")

    lazy val listCommand =Command(
      name = "list",
      header = "Lists known files"
    ) {
      (path, mismatch, gapMultiplier, delimiter).mapN { (p, m, g, d) =>
        val fileMap: Map[String, PWM] = LoaderPWM.load(p.toFile.toScala, m, g, d)
        println(s"${fileMap.size} PWMs processed for path ${p.toFile.toScala.path}!")
        for((f, pwm) <-fileMap){
          println(s"PWM found at ${f}")
          println("PWM values:")
          println(pwm)
          println(" \n")
        }
      }
    }


    val listSubcommand =  Opts.subcommand(listCommand)

    lazy val insertCommand = Command(
      name = "insert",
      header = "inserts sequence into PWM into the best place"
    ) {
      val insert = Opts.argument[String]("sequence")
      val number = Opts.option[Int](long = "num", short = "n", help = "number of insertions").withDefault(1)

      (insert, path, number, mismatch, gapMultiplier, delimiter).mapN { (i, p, n, m, g, d) =>
        val fileMap: Map[String, PWM] = LoaderPWM.load(p.toFile.toScala, m, g, d)
        println("PWMs processed")
        for((f, m) <-fileMap) println(s"file found: ${f}")
      }
    }

    val insertSubcommand = Opts.subcommand(insertCommand)

    (listSubcommand orElse insertSubcommand).map{ _=>

    }
  }


)
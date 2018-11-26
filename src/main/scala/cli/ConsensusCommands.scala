package cli


import java.nio.file.Path

import better.files._
import cats.implicits._
import com.monovore.decline._
import pwms.{LoaderPWM, PWM}

trait ConsensusCommands extends ListCommand{

  protected lazy val filePWM = Opts.argument[Path]("input PWM")
  protected lazy val gaps = Opts.flag(long = "gaps", help = "if gaps are allowed in the generated value").map(_=>true).withDefault(false)

  protected lazy val consensus = Command(
    name = "consensus",
    header = "gives the best consensus for the PWM (note by now shows only standard nucleotides, no intermediates)"
  ) {
    (filePWM, gaps, verbose, delimiter).mapN{ (f, g, verb, d)=>
      println(s"loading file ${f.toFile.toScala.path}")
      val pwm = LoaderPWM.loadFile(f, delimiter = d)
      if(verb){
        println("PWM values are:")
        println(pwm.toString)
      }
      println("consensus is:")
      println(pwm.consensus(true))
    }
  }

  protected val consensusSubcommand = Opts.subcommand(consensus)


  protected lazy val random = Command(
    name = "random",
    header = "gives random value generated from PWM"
  ) {
    (filePWM, gaps, verbose, delimiter).mapN{ (f, g, verb, d)=>
      println(s"loading file ${f.toFile.toScala.path}")
      val pwm = LoaderPWM.loadFile(f, delimiter = d)
      if(verb){
        println("PWM values are:")
        println(pwm.toString)
      }
      println("consensus is:")
      println(pwm.consensus(true))
    }
  }

  protected val randomSubcommand = Opts.subcommand(random)
}
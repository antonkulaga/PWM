package cli
import java.nio.file.Path

import assembly.cloning.{GoldenGate, RestrictionEnzyme, RestrictionEnzymes}
import better.files._
import cats.data.NonEmptyList
import cats.implicits._
import com.monovore.decline._
import pwms.{LoaderPWM, PWM, SequenceLoader}

import scala.collection.immutable.ListMap

object CloningCommands extends CloningCommands
trait CloningCommands extends MergeCommands {


  protected lazy val sequences = Opts.arguments[Path]("sequences")
  protected lazy val enzyme = Opts.option[String](long = "enzyme", short = "e", help = "Restriction enzyme to be used with GoldenGate, currently only BsaI (default) and BbsI are supported").withDefault("BsaI")

  protected lazy val left = Opts.option[String](long = "left", short = "l",
    help = "left ")
    .withDefault("")

  protected lazy val right = Opts.option[String](long = "right", short = "r",
    help = "Restriction enzyme to be used with GoldenGate, currently only BsaI (default) and BbsI are supported")
    .withDefault("")

  def enzymeFromName(enzyme: String): RestrictionEnzyme = enzyme
    match {
      case "BbsI" => RestrictionEnzymes.BbsI
      case "BsaI" => RestrictionEnzymes.BsaI
      case "BsmBI" => RestrictionEnzymes.BsmBI
      case "" =>
        RestrictionEnzymes.BsmBI
      case other =>
        println(s"$other enzyme is not supported by current GoldenGate implementation, selecting BsmBI instead")
        RestrictionEnzymes.BsmBI
  }

  def goldenClone(seq: NonEmptyList[Path], enzyme: String, output: Path, left: String, right: String)= {
    val restriction = enzymeFromName(enzyme)
    val gold = GoldenGate(restriction)
    val mp: ListMap[String, String] = SequenceLoader.loadFiles(seq.map(s=>s.toFile.toScala).toList)
    println("sequences to be connected together:")
    for((s, _) <- mp) println(s)
    println("-----------------")
    val it = mp.toList
    val fl = output.toFile.toScala
    val fasta = it.map(i=>(">"+i._1).replace(">>", ">")).zip(gold.synthesize(it.map(_._2), left, right)).map{ case (a, b)=> a + "\n" + b}.reduce(_ + "\n" + _)
    println(s"writing results as FASTA to ${fl.path}")
    fl.overwrite(fasta)
  }

  protected lazy val golden_gate: Command[Unit] = Command(
    name = "golden_gate",
    header = "Golden gate cloning between sequences"
  ) {
    (sequences, enzyme, outputFile, left, right).mapN{goldenClone}
  }

  val cloningSubcommand = Opts.subcommand(golden_gate)

}

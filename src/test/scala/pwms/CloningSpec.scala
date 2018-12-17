package pwms

import scala.util._
import better.files._
import File._
import java.io.{File => JFile}

import assembly.cloning.{GoldenGate, RestrictionEnzyme, RestrictionEnzymes}
import org.scalatest._
import cats.implicits._
import cli.CloningCommands

import scala.collection.immutable.ListMap
class CloningSpec extends WordSpec with Matchers  {


  "Cloning" should {
    "be accurate in Golden Gate" in {
      val lines1 = scala.io.Source.fromResource("to_gold.fasta").getLines().toList
      val mp: ListMap[String, String] = SequenceLoader.loadLines(lines1, "to_gold.fasta")
      val it = mp.toList
      println("====================")
      println(it)
      val left = "CCTG"
      val right = "GGAA"
      val restriction =  RestrictionEnzymes.BsaI
      val gold = GoldenGate(restriction)
      val oneS::twoS::threeS::Nil = gold.synthesize(it.map(_._2), left, right)
      val oneR: String = "GGTCTCCCCTGATTTGGAGAGCTCTGAAAATTTCGTACGAGACC"
      oneS shouldEqual oneR
      val twoR: String = "GGTCTCCCGTAGGAAATGTGAGCGCTCACAAATAAAATCAAGCGAGACC"
      twoS shouldEqual  twoR
      val threeR: String = "GGTCTCCCAAGACAGAAGCATTCTCAGAAACCTCTTTGTGGGAACGAGACC"
      threeS shouldEqual threeR
      val fasta: String = it.map(i=>(">"+i._1).replace(">>", ">"))
        .zip(gold.synthesize(it.map(_._2), left, right)).map{ case (a, b)=> a + "\n" + b}.reduce(_ + "\n" + _)
      val fastaR =""">one
GGTCTCCCCTGATTTGGAGAGCTCTGAAAATTTCGTACGAGACC
>two
GGTCTCCCGTAGGAAATGTGAGCGCTCACAAATAAAATCAAGCGAGACC
>three
GGTCTCCCAAGACAGAAGCATTCTCAGAAACCTCTTTGTGGGAACGAGACC"""
      fasta shouldEqual fastaR
      }
  }

}

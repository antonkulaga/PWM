package pwms

import scala.util._
import better.files._
import File._
import java.io.{File => JFile}
import org.scalatest._
import cats.implicits._

trait BasicPWMSpec {
  self: WordSpec with Matchers =>


  lazy val defaultTotalMiss: Double = -12.0
  lazy val defaultGapMultiplier = 4.0
  //lazy val gapMultiplier: Double = 4.0

  def loadTestPWM(totalMiss: Double = -defaultTotalMiss, gapMultiplier: Double = defaultGapMultiplier): PWM = {
    val lines1 = scala.io.Source.fromResource("to_parse.csv").getLines().toList
    PWM.parse(lines1,  totalMiss, gapMultiplier)
  }

  lazy val indexes: Map[Char, Int] = Map('A'-> 0, 'T'->1, 'G'->2, 'C'->3)
  lazy val indexesInv: Map[Int, Char] = indexes.map(_.swap)
  //protected def generateMismatches(num: Int) =


  def mismatch(seq: String, num: Int)(previous: Set[Int] = Set.empty): String = if(num == 0) seq else {
    val (i, x) = (Random.nextInt(seq.length), Random.nextInt(4))
    if(previous.contains(i) || indexes(seq(i)) == x)
      mismatch(seq, num)(previous)
    else
      mismatch(seq.updated(i, indexesInv(x)), num -1)(previous + i)
  }

  def countMismatches(a: String, b: String): Int = a.zip(b).map{ case (av, bv) => if(av == bv) 0 else 1}.sum
}

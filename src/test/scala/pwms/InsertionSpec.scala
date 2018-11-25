package pwms

import scala.util._
import better.files._
import File._
import java.io.{File => JFile}
import org.scalatest._
import cats.implicits._

class InsertionSpec extends WordSpec with Matchers {

  lazy val totalMiss: Double = -10.0
  //lazy val gapMultiplier: Double = 4.0

  def loadTestPWM(gapMultiplier: Double = 4.0): PWM = {
    val lines1 = scala.io.Source.fromResource("to_parse.csv").getLines().toList
    PWM.parse(lines1, ";", totalMiss, gapMultiplier)
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

  def testSearch(p: PWM, seq: String): (String, String) = {
    val half = p.matrix.cols / 2
    val a = Random.nextInt(half - seq.length -1 )
    val b = half + Random.nextInt(half - seq.length -1 )
    val updated = p.withInsertions(seq, 1000, a).withInsertions(seq, 1000, b)
    val ( a1, _)::(b1, _)::_ = updated.candidates(seq, 0, 0)
    val bestA = updated.readBest(a1, seq.length)
    val bestB = updated.readBest(b1, seq.length)

    assert(a == a1,
      s"""
         | wrong best index: $a != $a1
         | scores are: a = ${updated.scoreAt(seq, a)},  a1 = ${updated.scoreAt(seq, a1)}
         | sequence= ${seq}
         | a  read = ${updated.readBest(a, seq.length)}
         | a1 read = ${bestA}
      """.stripMargin)

    assert(b == b1,
      s"""
         | wrong best index: $b != $b1
         | scores are: a = ${updated.scoreAt(seq, b)},  a1 = ${updated.scoreAt(seq, b1)}
         | b  read = ${updated.readBest(b, seq.length)}
         | b1 read = ${bestB}
      """.stripMargin)
      bestA shouldEqual seq
      bestB shouldEqual seq
    (bestA, bestB)
  }

  def testSearch(p: PWM, seq: String, miss1: Int, miss2: Int): (String, String) = {
    val half = p.matrix.cols / 2
    val a = Random.nextInt(half - seq.length -1 )
    val b = half + Random.nextInt(half - seq.length -1 )
    val ins1 = mismatch(seq, miss1)()
    //println(seq, ins1)
    assert(countMismatches(ins1, seq) == miss1, s"insert should contain ${miss1} mismatches, original $seq, missmatched $ins1")
    val ins2 = mismatch(seq, miss2)()
    //println(seq, ins2)
    assert(countMismatches(ins2, seq) == miss2, s"insert should contain ${miss2} mismatches, original $seq, missmatched $ins1")

    val updated = p.withInsertions(ins1, 1000, a).withInsertions(ins2, 1000, b)
    ins1 shouldEqual updated.readBest(a, seq.length)
    ins2 shouldEqual updated.readBest(b, seq.length)

    val minScore: Double = totalMiss * miss2
    val ( a1, _)::(b1, _)::tail = updated.candidates(seq, 1, minScore)
    val bestA = updated.readBest(a1, seq.length)
    val bestB = updated.readBest(b1, seq.length)

    assert(a == a1,
      s"""
        | wrong best index: $a != $a1
        | scores are: a = ${updated.scoreAt(seq, a)},  a1 = ${updated.scoreAt(seq, a1)}
        | sequence= ${seq}
        | a  read = ${updated.readBest(a, seq.length)} with ${countMismatches(seq, updated.readBest(a, seq.length))} mismatches
        | a1 read = ${bestA} with ${countMismatches(seq, bestA)} mismatches
        | mismatches are ${miss1}
      """.stripMargin)

    assert(b == b1,
      s"""
         | wrong best index: $b != $b1
         | scores are: b = ${updated.scoreAt(seq, b)},  b1 = ${updated.scoreAt(seq, b1)}
         | sequence= ${seq}
         | b  read = ${updated.readBest(b, seq.length)} with ${countMismatches(seq, updated.readBest(b, seq.length))} mismatches
         | b1 read = ${bestB} with ${countMismatches(seq, bestB)} mismatches
         | mismatches are ${miss2}
      """.stripMargin)
    if(miss1 == 0 && miss2 ==0)
    {
      bestA shouldEqual seq
      bestB shouldEqual seq
    } else {
      assert(countMismatches(bestA, seq) <= miss1, s"best found results should contain no more than ${miss1} mismatches")
      assert(countMismatches(bestB, seq) <= miss2, s"best found results should contain no more than ${miss2} mismatches")
    }
    (bestA, bestB)
  }

  "PWM" should {
      "be parsed properly" in {
        val gapMultiplier = 4.0
        val p = loadTestPWM(gapMultiplier)
        val lines2: Seq[String] = p.toLines
        val p2: PWM = PWM.parse(lines2, delimiter = "\t", totalMiss, gapMultiplier)
        p shouldEqual p2
        val lines3 = scala.io.Source.fromResource("to_parse.tsv").getLines().toList
        val p3: PWM = PWM.parse(lines3, delimiter = "\t", totalMiss, gapMultiplier)
        p shouldEqual p3
        //    (lines: Seq[String], delimiter: String, totalMissScore: Double, skipGaps: Boolean = false)
      }

      "merge correctly" in {
        val p1 = loadTestPWM()
        val p2 = loadTestPWM()
        val p3 = p1 |+| p2
        p3.matrix shouldEqual p1.matrix + p2.matrix
      }


      "suggest proper positions for insertions without mismatches" in {
        val seq = "CATGTGGAATTGTGAGCGGATAACAATTTG"
        val p = loadTestPWM()
        for{ _ <- 0 to 1000} { testSearch(p, seq) }
      }

      "suggest proper positions for insertions with mismatches" in {
        val seq = "CATGTGGAATTGTGAGCGGATAACAATTTG"
        val p = loadTestPWM(0.0)
        for{ _ <- 0 to 1000} { testSearch(p, seq, 1, 2) }
        for{ _ <- 0 to 1000} { testSearch(p, seq, 2, 3) }
        for{ _ <- 0 to 1000} { testSearch(p, seq, 3, 4) }
      }

      "do not change insertions" in {
        val seq = "CATGTGGAATTGTGAGCGGATAACAATTTG"
        val toInsert = "CATGTGGAATTGTGAGCGGATAACAATTAC"
        val p = loadTestPWM(4.0)
        val pos = 30
        val updatedLow = p.withInsertions(seq, 696.0, 30)
        updatedLow.candidates(seq, 1).head._1 shouldEqual(30)
        val updatedHigh = p.withInsertions(seq, 10000, 30)
        assert(updatedLow.candidates(seq, 1) != 30, "insertion should take into consideration value")
      }
  }
}
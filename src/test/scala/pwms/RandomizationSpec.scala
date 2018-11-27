package pwms


import scala.util._
import better.files._
import File._
import java.io.{File => JFile}

import breeze.linalg._
import breeze.numerics._
import org.scalatest._
import pwms.functions._
import cats.implicits._


class RandomizationSpec  extends WordSpec with Matchers with BasicPWMSpec {



  "Randomize function" should {
    "generate nucleotides according to probabilities" in {
      val p0 = loadTestPWM() //"A", "C", "G", "T"
      val matrix = p0.matrix
      val zeros = DenseMatrix.zeros[Double](1, matrix.cols)
      val nogaps = matrix.replaceRows(zeros, 0)
      val noGaps = PWM(p0.indexes,nogaps, -10)
      val noA = PWM(p0.indexes,nogaps.replaceRows(zeros, 1), -10)
      val noC = PWM(p0.indexes,nogaps.replaceRows(zeros, 2), -10)
      val noG =PWM(p0.indexes, nogaps.replaceRows(zeros, 3), -10)
      val noT = PWM(p0.indexes,nogaps.replaceRows(zeros, 4), -10)

      for(_ <- 1 to 10) {
        val s = noGaps.getSample()
        assert(s.contains("A"))
        assert(s.contains("T"))
        assert(s.contains("G"))
        assert(s.contains("C"))
      }

      for(_ <- 1 to 1000) {
        val s = noA.getSample()
        assert(!s.contains("A"), "it should not contain A")
        assert(s.contains("T"))
        assert(s.contains("G"))
        assert(s.contains("C"))
      }

      for(_ <- 1 to 1000) {
        val s = noC.getSample()
        assert(!s.contains("C"), "it should not contain C")
        assert(s.contains("T"))
        assert(s.contains("G"))
        assert(s.contains("A"))
      }

      for(_ <- 1 to 1000) {
        val s = noT.getSample()
        assert(!s.contains("T"), "it should not contain T")
        assert(s.contains("C"))
        assert(s.contains("G"))
        assert(s.contains("A"))
      }

      for(_ <- 1 to 1000) {
        val s = noG.getSample()
        assert(!s.contains("G"), "it should not contain G")
        assert(s.contains("T"))
        assert(s.contains("C"))
        assert(s.contains("A"))
      }
    }
  }

}

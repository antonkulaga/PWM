package pwms

import scala.util._
import better.files._
import File._
import java.io.{File => JFile}
import org.scalatest._
import cats.implicits._


class GeneralPWMSpec extends WordSpec with Matchers with BasicPWMSpec {
  "PWM" should {
    "be parsed properly" in {
      val gapMultiplier = 4.0
      val p = loadTestPWM(totalMiss = defaultTotalMiss, gapMultiplier)
      val lines2: Seq[String] = p.toLines
      val p2: PWM = PWM.parse(lines2, defaultTotalMiss, gapMultiplier)
      p shouldEqual p2
      val lines3 = scala.io.Source.fromResource("to_parse.tsv").getLines().toList
      val p3: PWM = PWM.parse(lines3, defaultTotalMiss, gapMultiplier)
      p shouldEqual p3
      //    (lines: Seq[String], delimiter: String, totalMissScore: Double, skipGaps: Boolean = false)
    }

    "merge correctly" in {
      val p1 = loadTestPWM()
      val p2 = loadTestPWM()
      val p3 = p1 |+| p2
      p3.matrix shouldEqual p1.matrix + p2.matrix
    }
  }

}

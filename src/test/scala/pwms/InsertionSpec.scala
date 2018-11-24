package pwms

import scala.util._
import better.files._
import File._
import java.io.{File => JFile}
import org.scalatest._
import cats.implicits._

class InsertionSpec extends FlatSpec with Matchers {

  def loadTestPWM: PWM = {
    val lines1 = scala.io.Source.fromResource("to_parse.csv").getLines().toList
    PWM.parse(lines1, ";", -100.0, 10.0)
  }

  "PWM" should "be parsed properly" in {
    val p = loadTestPWM
    val lines2: Seq[String] = p.toLines
    val p2: PWM = PWM.parse(lines2, delimiter = "\t", -100.0, 10.0)
    p shouldEqual p2
    val lines3 = scala.io.Source.fromResource("to_parse.tsv").getLines().toList
    val p3: PWM = PWM.parse(lines3, delimiter = "\t", -100.0, 10.0)
    p shouldEqual p3
    //    (lines: Seq[String], delimiter: String, totalMissScore: Double, skipGaps: Boolean = false)
  }

  "PWM" should "merge correctly" in {
    val p1 = loadTestPWM
    val p2 = loadTestPWM
    val p3 = p1 |+| p2
    p3.matrix shouldEqual p1.matrix + p2.matrix
  }

  "PWM" should "merge correctly" in {
    val p1 = loadTestPWM
    val p2 = loadTestPWM
    val p3 = p1 |+| p2
    p3.matrix shouldEqual p1.matrix + p2.matrix
  }

  "PWM" should "insert sequence property" in {
    val p1 = loadTestPWM

  }

}

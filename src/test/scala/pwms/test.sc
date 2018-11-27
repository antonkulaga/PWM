import breeze.linalg.DenseMatrix
import breeze.math._
import breeze.linalg._
import breeze.numerics._
import pwms._
import pwms.functions._

lazy val defaultTotalMiss: Double = -12.0
lazy val defaultGapMultiplier = 4.0
//lazy val gapMultiplier: Double = 4.0

def loadTestPWM(totalMiss: Double = -defaultTotalMiss, gapMultiplier: Double = defaultGapMultiplier): PWM = {
  val lines1 = scala.io.Source.fromFile("/data/sources/PWM/files/to_parse.csv").getLines().toList
  PWM.parse(lines1,  totalMiss, gapMultiplier)
}

val p0 = loadTestPWM()
val matrix = p0.matrix

val zeros = DenseMatrix.zeros[Double](1, matrix.cols)
val nogaps = matrix.replaceRows(zeros, 0)
val noGaps = PWM(p0.indexes,nogaps, -10)
val noA = PWM(p0.indexes,nogaps.replaceRows(zeros, 1), -10)

val noC = PWM(p0.indexes,nogaps.replaceRows(zeros, 2), -10)
val noG =PWM(p0.indexes, nogaps.replaceRows(zeros, 3), -10)
val noT = PWM(p0.indexes,nogaps.replaceRows(zeros, 4), -10)
println("=========")
noA.matrix(::,0)
val m= noA.gapMatrix * noA.gapMultiplier

println("--------")
noA.relativeFrequencies(::,0)

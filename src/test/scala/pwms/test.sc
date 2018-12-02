import assembly.cloning.RestrictionEnzymes
import assembly.synthesis.{ContentGC, GenerationParameters}
import better.files.File
import breeze.linalg.DenseMatrix
import breeze.math._
import breeze.linalg._
import breeze.numerics._
import pwms._
import pwms.functions._

import scala.collection.immutable.ListMap

lazy val defaultTotalMiss: Double = -12.0
lazy val defaultGapMultiplier = 4.0
//lazy val gapMultiplier: Double = 4.0

def loadTestPWM(totalMiss: Double = -defaultTotalMiss, gapMultiplier: Double = defaultGapMultiplier): PWM = {
  val lines1 = scala.io.Source.fromFile("/data/sources/PWM/files/to_parse.csv").getLines().toList
  PWM.parse(lines1,  totalMiss, gapMultiplier)
}
def replaceNuc(matrix: DenseMatrix[Double], row: Int): DenseMatrix[Double] = {
  val zeros = DenseMatrix.zeros[Double](1, matrix.cols)
  val v = if(row == matrix.rows-1) row -1 else row + 1
  val cp = matrix(row, ::).inner.toDenseMatrix + matrix(v, ::)
  matrix.replaceRows(zeros, row).replaceRows(cp, v)
}

val p0 = loadTestPWM()
val cons = p0.consensus()
val cons_no_gaps = p0.consensus(false)

import assembly._
val avoid = Set( "BsaI" -> "GGTCTC", "BsbI" -> "CAACAC") //,  "BspTNI" -> "GGTCTC",  "Bso31I" -> "GGTCTC", "Eco31I" -> "GGTCTC"


//val avoid = Set( "BsaI" -> "GGTCTC",  "BspTNI" -> "GGTCTC",  "Bso31I" -> "GGTCTC", "Eco31I" -> "GGTCTC","BsbI" -> "CAACAC")

//val ch = GenerationParameters.apply(p0,  20, Set(""), ContentGC.default, RestrictionEnzymes.default)
//val goldenGate = RestrictionEnzymes(avoid)
//goldenGate.find(cons_no_gaps)
val merged = File("/data/HAC/High-CENPB-only/all_merged")
  .children.map(f=>LoaderPWM.load(f))
  .toList
val gold = RestrictionEnzymes.GOLDEN_GATE

val (name, pwm) = merged.head.head
name
pwm
gold.canCut(pwm.consensus(false), true)

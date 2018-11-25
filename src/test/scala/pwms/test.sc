import pwms.PWM
import breeze.math._
import breeze.linalg._
import breeze.numerics._

def loadTestPWM: PWM = {
  val lines1 = scala.io.Source.fromFile("/data/sources/PWM/files/to_parse.csv").getLines().toList
  PWM.parse(lines1, ";", -10.0, 10.0)
}
import scala.util.Random

val totalMiss = -10
val gapMultiplier = 4


def mismatch(seq: String, num: Int): String = if(num<=0) seq else {
  val (i, x) = (Random.nextInt(seq.length), Random.nextInt(4))
  val indexes = Array('A', 'T', 'G', 'C')
  mismatch(seq.updated(i, indexes(x)), num -1)
}
val p = loadTestPWM

//println(p.consensus)
//println(p.consensus.size, p.matrix.cols)
/*
println(p.logOddsTable.toString(100,100))
println("---------------")
println(p.weightedLogOddsTable.toString(100,100))
*/
println(p)
println("---------------")
val u = p.withInsertions("TGTGAGCGCTCACA", 10000, 2)
println(u)
println("===============")
println(p.meanCol, u.meanCol)


println(u.logOddsTable.toString(100,100))
println("---------------")
println(u.weightedLogOddsTable.toString(100,100))

import pwms.PWM
import breeze.math._
import breeze.linalg._
import breeze.numerics._

def loadTestPWM: PWM = {
  val lines1 = scala.io.Source.fromFile("/data/sources/PWM/files/to_parse.csv").getLines().toList
  PWM.parse(lines1, ";", -100.0, 10.0)
}

val p = loadTestPWM
println(p)
println("=========================")
val t = p.matrix(1,::)
println(t(0))
println(t(1))
println(t(2))

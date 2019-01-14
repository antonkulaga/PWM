package pwms.functions

import java.security.SecureRandom

import breeze.generic.UFunc
import breeze.linalg.support.CanTraverseValues
import breeze.linalg.support.CanTraverseValues.ValuesVisitor

import scala.util.Random

object randomize extends UFunc {
  val random = new SecureRandom()

  implicit def sumFromTraverseDoubles[T](implicit traverse: CanTraverseValues[T, Double]): Impl[T, Double] = {
    new Impl[T, Double] {
      def apply(t: T): Double = {
        val rand = random.nextDouble()
        //val rand = Random.nextDouble()
        var i = 0
        var res = -1.0
        var sum = 0.0
        traverse.traverse(t, new ValuesVisitor[Double] {
          def visit(a: Double): Unit = if(res == -1.0) {
            sum += a
            if(rand < sum) res = i
            i +=1
          }
          def zeros(count: Int, zeroValue: Double): Unit = {}
        })
        res
      }
    }
  }
}

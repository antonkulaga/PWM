package pwms

import breeze.linalg._
import breeze.numerics._
import breeze.stats._
import cats._
import cats.implicits._

package object functions {
  
  implicit class MatrixExt(matrix: DenseMatrix[Double]){
    def shift(num: Int): DenseMatrix[Double] = num match {
      case 0 => matrix
      case n if n> 0 =>
        val first: DenseMatrix[Double] = DenseMatrix.zeros[Double](matrix.rows, num)
        DenseMatrix.horzcat(first, matrix)
      case n if n < 0=>  DenseMatrix.horzcat(matrix, DenseMatrix.zeros[Double](matrix.rows, num))
    }

    def replaceColumns(sm: DenseMatrix[Double], pos: Int): DenseMatrix[Double] = {
      require(sm.rows == matrix.rows, "both original and inserted matrix should have same number of rows")
      val first: DenseMatrix[Double] = DenseMatrix.horzcat(matrix(::, 0 until pos), sm)
      val second: DenseMatrix[Double] = matrix(::, (pos + sm.cols) until matrix.cols )
      DenseMatrix.horzcat(first, second)
    }

    def replaceRows(sm: DenseMatrix[Double], pos: Int): DenseMatrix[Double] = {
      require(sm.cols == matrix.cols, "both original and inserted matrix should have same number of columns")
      val first: DenseMatrix[Double] = DenseMatrix.vertcat(matrix(0 until pos, ::), sm)
      val second: DenseMatrix[Double] = matrix((pos + sm.rows) until matrix.rows, :: )
      DenseMatrix.vertcat(first, second)
    }

    def skipRows(index: Int, num: Int = 1): DenseMatrix[Double] = {
      require(index + num < matrix.rows, s"cannot smip rows ${index} to ${index + num} some of which are not in the matrix with row number ${matrix.rows}")
      val before: DenseMatrix[Double] = matrix(0 until index, ::)
      val after: DenseMatrix[Double] = matrix(index + num until matrix.rows, ::)
      DenseMatrix.vertcat(before, after)
    }
  }



}

package pwms
import breeze.linalg._
import breeze.numerics._
import cats._
import cats.implicits._

import scala.collection.{SortedMap, immutable}


/**
  * Companion object for PWM with some useful static methods
  */
object PWM {

  def addMatrices(a: DenseMatrix[Double], b: DenseMatrix[Double]): DenseMatrix[Double] =
    if(a.cols == b.cols)
      a + b
    else
      if(a.cols < b.cols)
        DenseMatrix.horzcat(a, DenseMatrix.zeros[Double](a.rows, b.cols - a.cols)) + b
      else
        addMatrices(b, a)

  /**
    * Semigroup for adding two PWMs together
    */
  implicit val pwmSemigroup: Semigroup[PWM] = new Semigroup[PWM] {
    def combine(x: PWM, y: PWM): PWM = {
      require(x.indexes == y.indexes, "Indexes of both PWM should be equal!")
      x.copy(matrix = addMatrices(x.matrix, y.matrix))
    }
  }


  def parse(lines: Seq[String], delimiter: String, totalMissScore: Double, gapMultiplier: Double = 10): PWM= {
    require(lines.nonEmpty, "Matrix cannot be empty")
    val h = lines.head
    val skipGaps = gapMultiplier == 0.0
    if(h.isEmpty || h=="\n" || h.startsWith(s""""V1"${delimiter}"V2"${delimiter}""")) parse(lines.tail, delimiter, totalMissScore, gapMultiplier) else {
      val mat= lines.map(_.split(delimiter))
      val namedRows = mat.map(line=> line.head.replace("\"", "") -> line.tail).filter(_._1 != "-" || !skipGaps).sortBy(_._1)
      val indexes: SortedMap[String, Int] = SortedMap(namedRows.map(_._1).zipWithIndex:_*)
      val rows = namedRows.map(_._2.map(v=>v.toDouble))
      val matrix: DenseMatrix[Double] = DenseMatrix(rows:_*)
      PWM(indexes, matrix, totalMissScore, gapMultiplier)
    }
  }

  def matrixToLines(indexes: SortedMap[String, Int], mat: DenseMatrix[Double], delimiter: String = "\t"): Seq[String] ={
    val rev = indexes.map{ case (k, v) => (v, k)}
    mat.toString(mat.rows * 2, 10000)
      .split("\n")
      .zipWithIndex.map{ case (s, i) => rev(i)  + delimiter + s.replaceAll("\\s{1,}", delimiter)}
  }

  def matrixToString(indexes: SortedMap[String, Int], mat: DenseMatrix[Double], delimiter: String = "\t"): String ={
    matrixToLines(indexes, mat, delimiter).reduce(_ + "\n" + _)
  }

}

/**
  * PWM class
  * @param indexes correspondance of nucleotides to Matrix rows
  * @param matrix matrix with values of the PWM
  * @param totalMissScore penalty for the mismatches
  * @param gapMultiplier make gaps easier for insertions
  */
case class PWM(indexes: SortedMap[String, Int], matrix: DenseMatrix[Double], totalMissScore: Double, gapMultiplier: Double = 5.0) {

  def apply(str: String): Transpose[DenseVector[Double]] = {
    require(indexes.contains(str), s"indexes do not contation ${str} \n indexes are:\n ${indexes.toString}")
    val num = indexes(str)
    matrix(num, ::)
  }

  /**
    * Moves PWM right by adding zero columns to the left
    * @param num columns number to add
    * @return new PWM
    */
  def shift(num: Int, values: Double): PWM = num match {
    case 0 => this
    case n if n> 0 => this.copy(matrix = DenseMatrix.horzcat(DenseMatrix.zeros[Double](matrix.rows, num), matrix) )
    case n if n < 0=> this.copy(matrix = DenseMatrix.horzcat(matrix, DenseMatrix.zeros[Double](matrix.rows, num)) )
  }

  def hasGaps: Boolean = indexes.contains("-")

  //zero if no gaps
  lazy val gapRow: DenseMatrix[Double] = if(hasGaps) matrix(indexes("-"), ::).inner.asDenseMatrix else  DenseMatrix.zeros[Double](1, matrix.cols)
  protected lazy val gapMatrix: DenseMatrix[Double] = tile(gapRow, matrix.rows, 1)

  protected lazy val oneColumn: DenseMatrix[Double] = DenseMatrix.ones[Double](matrix.rows, 1)
  protected lazy val oneRow: DenseMatrix[Double] = DenseMatrix.ones[Double](1, matrix.cols)

  protected lazy val backgroundColumn: DenseMatrix[Double] = oneColumn / (matrix.rows.toDouble - hasGaps.compare(false))

  protected lazy val backgroundMatrix: DenseMatrix[Double] = tile(backgroundColumn, 1, matrix.cols)

  lazy val oddsTable: DenseMatrix[Double] =  (matrix +:+ (gapMatrix * gapMultiplier) ) / backgroundMatrix

  lazy val logOddsTable: DenseMatrix[Double] = log( oddsTable ).map(v=> if(v.isInfinity) totalMissScore else v)

  def sequenceToMatrix(seq: String, value: Double = 1.0): DenseMatrix[Double] = {
    val m = DenseMatrix.zeros[Double](matrix.rows, seq.length)
    val ind: Seq[(Char, Int)] = seq.zipWithIndex
    for{
      (s, c) <- ind
      r = indexes(s.toString)
    } m.update(r, c, value)
    m
  }

  /**
    * Converts sequence into PWM matrix
    * @param seq
    * @return
    */
  def sequenceToPWM(seq: String, value: Double = 1.0) = PWM(indexes, sequenceToMatrix(seq, value), totalMissScore, gapMultiplier)

  def slideOdds(window: Int): immutable.IndexedSeq[DenseMatrix[Double]] = {
    for { i <- 0 until matrix.cols - window } yield logOddsTable(::, i until i + window)
  }

  def slideSequence(seq: String): Seq[(Int, Double)] = {
    val sm = sequenceToMatrix(seq)
    val window = sm.cols
    for {
      i <- 0 until matrix.cols - window
      score = sum(logOddsTable(::, i until i + window) *:* sm)
    } yield i -> score
  }

  def candidates(seq: String, num: Int, between: Int): Seq[(Int, Double)] = {
    val c = slideSequence(seq).filter(_._2 > 0.0).sortWith{ case ((_, a), (_, b)) => a > b}
    val window = seq.length
    (c.head :: c.sliding(2).filter(s=>s.head._1 + window + between < s.last._1).map(_.last).toList).take(num)
  }

  def candidatePWM(seq: String, num: Int, between: Int): (PWM, Seq[Int]) = {
    val sm = sequenceToMatrix(seq)
    val cands = candidates(seq, num, between)
    cands.foldLeft(this){
      case (acc, (i, _)) => acc.insertPWM(sm, i)
    } -> cands.map(_._1)
  }

  def insert(seq: String, index: Int, value: Double = 1.0): PWM = {
    val sm = sequenceToMatrix(seq, value)
    insertPWM(sm, index)
  }

  def insertPWM(sm: DenseMatrix[Double], pos: Int): PWM = {
    PWM(indexes, insert(matrix, sm, pos), totalMissScore, gapMultiplier)
  }

  def insert(m: DenseMatrix[Double], sm: DenseMatrix[Double], pos: Int): DenseMatrix[Double] = {
    DenseMatrix.horzcat(DenseMatrix.horzcat(m(::, 0 until pos), sm), m(::, (pos + sm.cols) until m.cols ))
  }

  override def toString: String = PWM.matrixToString(indexes, this.matrix)
  lazy val toLines: Seq[String] = PWM.matrixToLines(indexes, this.matrix)

  lazy val sumRows: DenseMatrix[Double] = matrix * oneRow.t
  lazy val sumCols: DenseMatrix[Double] = oneColumn.t * matrix
  //lazy val relativeFrequencies: DenseMatrix[Double] = matrix /:/  (oneColumn * sumCols)

}


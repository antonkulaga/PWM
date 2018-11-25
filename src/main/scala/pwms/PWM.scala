package pwms
import breeze.linalg._
import breeze.numerics._
import breeze.stats._
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


  def insertMatrix(m: DenseMatrix[Double], sm: DenseMatrix[Double], pos: Int): DenseMatrix[Double] = {
    DenseMatrix.horzcat(DenseMatrix.horzcat(m(::, 0 until pos), sm), m(::, pos until m.cols ))
  }

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

  val indexesInverted: SortedMap[Int, String] = indexes.map(_.swap)

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

  lazy val backgroundMatrix: DenseMatrix[Double] = tile(backgroundColumn, 1, matrix.cols)

  lazy val sumRows: DenseMatrix[Double] = matrix * oneRow.t
  lazy val sumCols: DenseMatrix[Double] = oneColumn.t * matrix
  lazy val meanCol: Double = mean(sumCols)
  lazy val colWeights: DenseMatrix[Double] = tile(sumCols / meanCol, matrix.rows, 1 )

  //lazy val meanSumCols: DenseMatrix[Double] = { DenseMatrix.fill[Double](1, matrix.cols)(meanCol)}

  lazy val relativeFrequencies: DenseMatrix[Double] = (matrix  +:+ (gapMatrix * gapMultiplier)) /:/  (oneColumn * sumCols)

  lazy val oddsTable: DenseMatrix[Double] =  relativeFrequencies / backgroundMatrix

  //lazy val oddsTable: DenseMatrix[Double] =  relativeFrequencies / backgroundMatrix

  lazy val logOddsTable: DenseMatrix[Double] = log( oddsTable ).map(v=>if(v.isInfinity) totalMissScore else v )
  lazy val negativeLogOdds: DenseMatrix[Double] = logOddsTable.map(v => if(v < 0) v else 0.0)
  lazy val positiveLogOdds: DenseMatrix[Double] = logOddsTable.map(v => if(v >= 0) v else 0.0)

  //.map{ case v => if(v.isInfinity) totalMissScore else v}
  //logOddsTable *:* colWeights
  lazy val weightedLogOddsTable: DenseMatrix[Double] =  positiveLogOdds + (negativeLogOdds  *:*  colWeights) //just a hack to increase importance of high values

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
    * @param sequence
    * @return
    */
  def sequenceToPWM(sequence: String, value: Double) = PWM(indexes, sequenceToMatrix(sequence, value), totalMissScore, gapMultiplier)

  def slideOdds(window: Int): immutable.IndexedSeq[DenseMatrix[Double]] = {
    for { i <- 0 until matrix.cols - window } yield weightedLogOddsTable(::, i until i + window)
  }

  protected def slideSequence(sequence: String): Seq[(Int, Double)] = {
    val sm = sequenceToMatrix(sequence)
    val window = sm.cols
    for {
      i <- 0 until matrix.cols - window
      score = scoreAt(sm, i)
    } yield i -> score
  }

  def scoreAt(sequence: String, i: Int): Double = {
    val sm = sequenceToMatrix(sequence)
    scoreAt(sm, i)
  }
  def scoreAt(sm: DenseMatrix[Double], i: Int): Double = {
    val window = sm.cols
    sum(weightedLogOddsTable(::, i until i + window) *:* sm)
  }

  lazy val consensus = readBest(0, this.matrix.cols)

  def readBest(position: Int, length: Int): String = {
    val m: DenseMatrix[Double] = matrix(::, position until (position + length))
    val args: Transpose[DenseVector[Int]] = argmax(m(::, *))
    (for(i <- args.inner) yield indexesInverted(i)).reduce(_ + _)
  }

  /**
    * Computes and ranks candidate insertion places that do not intersect and have a distance between them
    * @param seq sequence insertion place
    * @param distance minimal distance between insertions
    * @return
    */
  def candidates(seq: String, distance: Int = 0, minimalScore: Double = 0.0): List[(Int, Double)] = {
    val slide: Seq[(Int, Double)] = slideSequence(seq).filter(_._2 > minimalScore)
    val slides: List[(Int, Double)] = slide.foldLeft(List.empty[(Int, Double)]){
      case (Nil, (i, v)) => (i, v)::Nil
      case ( (i0, v0)::tail, (i, v) ) if i0 + distance + seq.length < i => (i, v)::(i0, v0)::tail
      case ( (i0, v0)::tail, (_, v) ) if v0 >= v => (i0, v0)::tail
      case ((_, v0)::tail, (i, v)) if v0 < v => (i, v)::tail
    }
    slides.sortWith{ case ((i0, v0), (i, v)) => (v0 == v && i0 <= i) || v0 > v}
  }

  /**
    * new PWM with insertions of the sequence into one or more positions
    * @param sequence sequence to be inserted
    * @param value value with wich to fill in sequence PWM
    * @param positions positions to which make the insertions
    * @return
    */
  def withInsertions(sequence: String, value: Double, positions: Int*): PWM = {
    val sm = sequenceToMatrix(sequence, value)
    val newMat = positions.foldLeft(this.matrix){ case (acc, pos) => PWM.insertMatrix(acc, sm, pos) }
    copy(matrix = newMat)
  }

  override def toString: String = PWM.matrixToString(indexes, this.matrix)
  lazy val toLines: Seq[String] = PWM.matrixToLines(indexes, this.matrix)


}


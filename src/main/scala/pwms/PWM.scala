package pwms
import better.files.File
import breeze.linalg._
import breeze.numerics._
import breeze.stats._
import cats._
import cats.implicits._
import pwms.functions._
import scala.compat._

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


  /**
    * Parses lines of strings as PWM
    * @param lines
    * @param totalMissScore
    * @param gapMultiplier
    * @param delimiter
    * @return
    */
  def parse(lines: Seq[String], totalMissScore: Double, gapMultiplier: Double = 10, delimiter: String = "auto"): PWM= {
    require(lines.nonEmpty, "Matrix cannot be empty")
    val h = lines.head
    val del = if(delimiter=="auto") {
      Map(h.count(_=='\t') -> "\t", h.count(_==';') -> ";", h.count(_==',') ->",").maxBy(_._1)._2
    } else delimiter
    val skipGaps = gapMultiplier == 0.0
    if(h.isEmpty || h=="\n" || h.startsWith(s""""V1"${del}"V2"${del}""")) parse(lines.tail, totalMissScore, gapMultiplier, del) else {
      val mat= lines.map(_.split(del))
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
      .replace(delimiter+"\n", "\n") //fix extra delimiter bug
  }

}

/**
  * PWM class
  * @param indexes correspondance of nucleotides to Matrix rows
  * @param matrix matrix with values of the PWM
  * @param totalMissScore penalty for the mismatches
  * @param gapMultiplier make gaps easier for insertions
  */
case class PWM(indexes: SortedMap[String, Int], matrix: DenseMatrix[Double], totalMissScore: Double, gapMultiplier: Double = 5.0) extends assembly.synthesis.SequenceTemplate {

  //lazy val matrixNoGaps: DenseMatrix[Double] = if(hasGaps) matrix.skipRows(indexes("-"), 1) else matrix
  //lazy val noGaps =
 // private lazy val columnOnesNoGaps = DenseMatrix.ones[Double](matrixNoGaps.rows, 1)

  val indexesInverted: SortedMap[Int, String] = indexes.map(_.swap)

  def apply(str: String): Transpose[DenseVector[Double]] = {
    require(indexes.contains(str), s"indexes do not contation ${str} \n indexes are:\n ${indexes.toString}")
    val num = indexes(str)
    matrix(num, ::)
  }

  def hasGaps: Boolean = indexes.contains("-")
  //lazy val noGapsMatrix = if(hasGaps) matrix.fil else matrix

  //zero if no gaps
  lazy val gapRow: DenseMatrix[Double] = if(hasGaps) matrix(indexes("-"), ::).inner.asDenseMatrix else  DenseMatrix.zeros[Double](1, matrix.cols)
  lazy val gapMatrix: DenseMatrix[Double] = tile(gapRow, matrix.rows, 1)


  lazy val backgroundMatrix: DenseMatrix[Double] = DenseMatrix.ones[Double](matrix.rows, matrix.cols) /:/ (matrix.rows.toDouble - hasGaps.compare(false))

  lazy val sumRows: DenseMatrix[Double] = sum(matrix(*, ::)).toDenseMatrix
  lazy val sumCols: DenseMatrix[Double] = sum(matrix(::, *)).inner.toDenseMatrix

  lazy val meanCol: Double = mean(sumCols)
  lazy val colWeights: DenseMatrix[Double] = tile(sumCols / meanCol, matrix.rows, 1 )
  lazy val relativeFrequencies: DenseMatrix[Double] = (matrix  +:+ (gapMatrix * gapMultiplier)) /:/  tile(sumCols, matrix.rows, 1)


  protected lazy val gapsMatrixPure: DenseMatrix[Double] = tile(gapRow, matrix.rows, 1) /:/ (matrix.rows -1).toDouble

  lazy val matrixNoGaps: DenseMatrix[Double] = matrix.replaceRows(DenseMatrix.zeros[Double](1, matrix.cols), indexes("-"))

  lazy val sumColsNoGaps: DenseMatrix[Double] = sum(matrixNoGaps(::, *)).inner.toDenseMatrix


  lazy val relativeFrequenciesPure: DenseMatrix[Double] = if(hasGaps)
    (matrixNoGaps /*+ gapsMatrixPure*/ ) /:/  tile(sumColsNoGaps, matrixNoGaps.rows, 1)
  else
    relativeFrequencies

  lazy val oddsTable: DenseMatrix[Double] =  relativeFrequencies / backgroundMatrix

  lazy val logOddsTable: DenseMatrix[Double] = log( oddsTable ).map(v=>if(v.isInfinity) totalMissScore else v )
  lazy val negativeLogOdds: DenseMatrix[Double] = logOddsTable.map(v => if(v < 0) v else 0.0)
  lazy val positiveLogOdds: DenseMatrix[Double] = logOddsTable.map(v => if(v >= 0) v else 0.0)

  lazy val weightedLogOddsTable: DenseMatrix[Double] =  positiveLogOdds + (negativeLogOdds  *:*  colWeights) //just a hack to increase importance of high values

  /**
    * 'W' -> Array('A', 'T'), //weak
    * 'S' -> Array('C', 'G'), //strong
    * 'M' -> Array('A', 'C'), //amino
    * 'K' -> Array('G', 'T'), //keto
    * 'R' -> Array('A', 'G'), //purine
    * 'Y' -> Array('C', 'T'), //pyramidine
    * 'B' -> Array('C', 'G', 'T'), //not A
    * 'D' -> Array('A', 'G', 'T'), //not C
    * 'H' -> Array('A', 'C', 'T'), //not G
    * 'V' -> Array('A', 'C', 'G'), //not T
    * 'N' -> Array('A', 'C', 'G', 'T') //any
    *
    * @param seq
    * @param value
    * @return
    */
  lazy val nucMap: Map[Char, Array[Char]] = assembly.extensions.nucs ++ Map('A' -> Array('A'),'T' -> Array('T'),'G' -> Array('G'),'C' -> Array('C') )

  def sequenceToMatrix(seq: String, value: Double): DenseMatrix[Double] = {
    val m = DenseMatrix.zeros[Double](matrix.rows, seq.length)
    val ind: Seq[(Char, Int)] = seq.toUpperCase.zipWithIndex
    for{
      (s, c) <- ind
      arr = nucMap(s)
      n <- arr
      r = indexes(n.toString)
    } m.update(r, c, value / arr.length)
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

  protected def slideSequence(sequence: String, mult: Double = 1.0, begin: Int = 0, end: Int = Int.MaxValue): Seq[(Int, Double)] = {
    val sm: DenseMatrix[Double] = sequenceToMatrix(sequence, mult)
    slideMatrix(sm, mult, begin, end)
  }

  protected def slideMatrix(sm: DenseMatrix[Double], mult: Double = 1.0, begin: Int = 0, end: Int = Int.MaxValue): Seq[(Int, Double)] = {
    val finish = Math.min(matrix.cols - sm.cols, end)
    for { i <- begin until finish } yield i -> scoreAt(sm, i)
  }

  def scoreAt(sequence: String, i: Int): Double = {
    val sm = sequenceToMatrix(sequence, 1.0)
    scoreAt(sm, i)
  }

  def scoreAt(sm: DenseMatrix[Double], i: Int): Double = {
    val end = sm.cols + i
    sum(weightedLogOddsTable(::, i until end) *:* sm)
  }

  def consensus(gaps: Boolean = true): String = readBest(0, this.matrix.cols, gaps)

  def readBest(position: Int, length: Int, gaps: Boolean = true): String = {
    //val mat = if(gaps)
    val mat = if(gaps) matrix else matrixNoGaps
    val m: DenseMatrix[Double] = mat(::, position until (position + length))
    val args: Transpose[DenseVector[Int]] = argmax(m(::, *))
    (for(i <- args.inner) yield indexesInverted(i)).reduce(_ + _)
  }

  /**
    * Computes and ranks candidate insertion places that do not intersect and have a distance between them
    * @param seq sequence insertion place
    * @param distance minimal distance between insertions
    * @return
    */
  def candidates(seq: String, distance: Int = 0, begin: Int = 0, end: Int = Int.MaxValue, mult: Int = 1): List[(Int, Double)] = {
    val sm: DenseMatrix[Double] = sequenceToMatrix(seq, mult)
    candidates(sm, distance, begin, end)
  }

  def candidates(sm: DenseMatrix[Double], distance: Int, begin: Int, end: Int): List[(Int, Double)] = {
    val slide: Seq[(Int, Double)] = slideMatrix(sm, begin = begin, end = end)//.filter(_._2 > minimalScore)
    val slides: List[(Int, Double)] = slide.foldLeft(List.empty[(Int, Double)]){
      case (Nil, (i, v)) => (i, v)::Nil
      case ( (i0, v0)::tail, (i, v) ) if i0 + distance + sm.cols < i => (i, v)::(i0, v0)::tail
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
  def withReplacement(sequence: String, value: Double, positions: Int*): PWM = {
    val sm: DenseMatrix[Double] = sequenceToMatrix(sequence, value)
    withReplacement(sm, value, positions:_*)
  }

  def withReplacement(sm: DenseMatrix[Double], value: Double, positions: Int*): PWM = {
    val newMat = positions.foldLeft(this.matrix){ case (acc, pos) => acc.replaceColumns(sm, pos) }
    copy(matrix = newMat)
  }

  override def toString: String = toString("\t")
  def toString(delimiter: String): String = PWM.matrixToString(indexes, this.matrix, delimiter)
  lazy val toLines: Seq[String] = PWM.matrixToLines(indexes, this.matrix)

  def write(file: File, delimiter: String = "\t", overwrite: Boolean = true): File = {
    val str: String = this.toString(delimiter)
    if(overwrite) file.overwrite(str) else file.write(str)
    file
  }

  def randomize: String = getSample

  def getSample: String = {
    pwms.functions.randomize(relativeFrequenciesPure(::, *)).inner.map(v=>indexesInverted(v.toInt)).reduce(_ + _)
  }

}

package pwms
import java.io.FileNotFoundException
import java.nio.file.Path

import scala.util._
import better.files._
import File._
import cats.implicits._
import com.monovore.decline._

import scala.collection.immutable.ListMap

object LoaderPWM extends LoaderPWM
trait LoaderPWM {

  def loadPath(path: String, totalMissScore: Double = -12.0, gapMultiplier: Double = 4.0, delimiter: String = "auto", extensions: Set[String] = Set(".pwm", ".tsv", ".csv")) =
    load(File(path), totalMissScore, gapMultiplier, delimiter, extensions)
  def load(file: File, totalMissScore: Double = -12.0, gapMultiplier: Double = 4.0, delimiter: String = "auto", extensions: Set[String] = Set(".pwm", ".tsv", ".csv")): ListMap[String, PWM] ={
    file match {
      case f if f.isRegularFile =>
        ListMap(f.path.toString -> PWM.parse(f.lines.toList, totalMissScore, gapMultiplier, delimiter))

      case dir if dir.isDirectory =>
        val children = dir.children
          .filter(f=> f.isDirectory || (f.isRegularFile && f.hasExtension && extensions.contains(f.extension.get)))
          .flatMap(f=>load(f, totalMissScore, gapMultiplier, delimiter, extensions))
        //ListMap(dir.children.flatMap(f=>load(f, delimiter, totalMissScore, gapMultiplier)):_*)
        ListMap[String, PWM](children.toList:_*)

      case _ => throw new FileNotFoundException(s"cannot find file or folder called ${file}!")
    }
  }

}

package pwms
import java.io.FileNotFoundException

import better.files._

import scala.collection.immutable.ListMap

/**
  * Class that loads PWMs from files and folders
  */
object LoaderPWM extends LoaderPWM
trait LoaderPWM {

  def loadFile(file: File, totalMissScore: Double = -12.0, gapMultiplier: Double = 4.0, delimiter: String = "auto"): PWM = {
    PWM.parse(file.lines.toList, totalMissScore, gapMultiplier, delimiter)
  }

  def loadPath(path: String, totalMissScore: Double = -12.0, gapMultiplier: Double = 4.0, delimiter: String = "auto", extensions: Set[String] = Set(".pwm", ".tsv", ".csv")): ListMap[String, PWM] =
    load(File(path), totalMissScore, gapMultiplier, delimiter, extensions)

  def load(file: File, totalMissScore: Double = -12.0, gapMultiplier: Double = 4.0, delimiter: String = "auto", extensions: Set[String] = Set(".pwm", ".tsv", ".csv")): ListMap[String, PWM] ={
    file match {
      case f if f.isRegularFile =>
        ListMap(f.path.toString -> loadFile(f))

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

package pwms
import java.io.FileNotFoundException
import java.nio.file.Path

import scala.util._
import better.files._
import File._
import cats.implicits._
import com.monovore.decline._

import scala.collection.immutable.ListMap

object SequenceLoader extends SequenceLoader

class SequenceLoader {

  def loadLines(lines: Seq[String], name: String): ListMap[String, String] = {
    lines.foldLeft(ListMap.empty[String, String]){
      case (mp, el) if el.contains(">") => mp.updated(el, "")
      case (mp, el) if mp.isEmpty => mp.updated(name, el)
      case (mp, el) => mp.updated(mp.last._1, mp.last._2 + el)
    }
  }


  def loadFile(file: File): ListMap[String, String] = loadLines(file.lines.toList, file.name)

  def loadFiles(files: Seq[File], extensions: Set[String] = Set(".txt", ".fasta", ".dna", ".seq")): ListMap[String, String] = {
    val mps = files.map(f=>load(f))
    if(mps.size < 2) mps.headOption.getOrElse(ListMap.empty[String, String]) else mps.reduce(_ ++ _)
  }

  def load(file: File, extensions: Set[String] = Set(".txt", ".fasta", ".dna", ".seq")): ListMap[String, String] ={
    file match {
      case f if f.isRegularFile => loadFile(f)
      case dir if dir.isDirectory =>
        val mps = dir.children
          .filter(f=> f.isDirectory || (f.isRegularFile && f.hasExtension && extensions.contains(f.extension.get)))
          .map(f=>loadFile(f)).toList
        if(mps.size < 2) mps.headOption.getOrElse(ListMap.empty[String, String]) else mps.reduce(_ ++ _)

      case _ => throw new FileNotFoundException(s"cannot find file or folder called ${file}!")
    }
  }
}
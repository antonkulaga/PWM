import better.files.File
import pwms.{LoaderPWM, PWM}
import breeze.math._
import breeze.linalg._
import breeze.numerics._
import java.nio.file.Path

import scala.util._
import better.files._
import File._
import cats.implicits._
import com.monovore.decline._


val files = LoaderPWM.load(File("/data/sources/PWM/files"),";")
for(f <- files)
  println(f)
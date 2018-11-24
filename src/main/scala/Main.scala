package chromosome

import java.nio.file.Path
import scala.util._
import better.files._
import File._
import cats.implicits._
import com.monovore.decline._

object Main  extends CommandApp(
  name = "PWM",
  header = "PWM inserter",
  main = {
    val insert =
      Opts.option[String]("insert", short = "i", help = "Sequence to be inserted")
    val number = Opts.option[Int](long = "quantity", short = "q", help = "number of insertions").withDefault(1)
    val path = Opts.option[Path]( long = "path", short = "p", help = "file or folder inside which to make insertions", metavar = "file")

    (insert, number, path).mapN { (i, n, p) =>
      p.toFile.toScala match {
        case f if f.isRegularFile => println(s"${f} is a file!")
        case dir if dir.isDirectory => println(s"${dir} is a folder!")
        case _ => println(s"cannot find file or folder called ${p}!")
      }


      println("PWMs processed")
    }
  }
)
package untyped
package closure

import com.google.javascript.jscomp._
import sbt._
import scala.collection._

import com.samskivert.mustache.{Mustache,Template}

object JsSource {

  val importRegex = """   //[ \t]*require[ \t]*"([^"]+)"   """.trim.r

}

case class JsSource(val src: File, val des: File) extends Source {
  
  def isJsm = false

  val srcDirectory = new File(src.getParent)
  
  lazy val imports: Seq[File] =
    for {
      line <- IO.readLines(src).map(_.trim).toList
      name <- JsSource.importRegex.findAllIn(line).matchData.map(_.group(1)).toList
    } yield new File(srcDirectory, name).getCanonicalFile

  /** Closure sources for this file (not its imports or parents). */
  def closureSources: List[JSSourceFile] =
    // TODO: If Mustache is enabled, transform and use JsSourceFile.fromString instead:
    List(JSSourceFile.fromFile(src))
    
}

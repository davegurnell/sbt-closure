package untyped
package closure

import com.google.javascript.jscomp._
import sbt._
import scala.collection._

import com.samskivert.mustache.{Mustache,Template}

object JsSource {

  val importRegex =
    """
    //[ \t]*require[ \t]*"(.+)"$
    """.trim.r

}

case class JsSource(val sources: Sources, val src: File, val des: File) extends Source {
  
  lazy val parents: List[Source] =
    for {
      line <- IO.readLines(src).map(_.trim).toList
      name <- JsSource.importRegex.findAllIn(line).matchData.map(_.group(1)).toList
    } yield sources.getSource(name, this)

  /** Closure sources for this file (not its imports or parents). */
  def closureSources: List[JSSourceFile] =
    // TODO: If Mustache is enabled, transform and use JsSourceFile.fromString instead:
    List(JSSourceFile.fromFile(src))
    
}

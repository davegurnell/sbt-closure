package untyped
package closure

import com.google.javascript.jscomp._
import sbt._
import scala.collection._

case class JsmSource(val sources: Sources, val src: File, val des: File) extends Source {
  
  lazy val parents: List[Source] =
    for {
      line <- lines.map(stripComments _).filterNot(isSkippable _)
    } yield sources.getSource(line, this)

  /** Closure sources for this file (not its imports or parents). */
  def closureSources: List[JSSourceFile] =
    // TODO: If Mustache is enabled, transform and use JsSourceFile.fromString instead:
    Nil
  
  // Downloading URLs ---------------------------
  
  //def download(log: Logger, url: String): File = {
  //  val tempDirectory = des.getParent
  //}
  
  // Helpers ------------------------------------

  /** Strip JSM comments (that start with a # symbol). */
  def stripComments(line: String): String =
    "#.*$".r.replaceAllIn(line, "").trim
  
  /** Is this line skippable - i.e. does it not contain content? Assumes comments have been stripped. */
  def isSkippable(line: String): Boolean =
    line == ""
  
  /** Is this line a URL? Assumes comments have been stripped. */
  def isUrl(line: String): Boolean =
    line.matches("^https?:.*")
  
}

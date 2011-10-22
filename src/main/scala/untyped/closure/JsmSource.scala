package untyped
package closure

import com.google.javascript.jscomp._
import sbt._
import scala.collection._

case class JsmSource(val src: File, val des: File) extends Source {
  
  def isJsm = false
  
  lazy val imports: List[File] = {
    val srcDirectory = src.getParent

    lines.map(stripComments _).
          filterNot(isSkippable _).
          map { line =>
            //if(isUrl(line)) {
            //  download(log, line) 
            //} else {
              new File(srcDirectory, line)
            //}
          }
  }
  
  def closureSources: List[JSSourceFile] = 
    // TODO: Mustache
    //if(closureJsIsTemplated(path)) 
    //  imports.map(in => JSSourceFile.fromCode(in, renderTemplate(path))
    //else
    imports.map(JSSourceFile.fromFile(_))
  
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

package untyped
package closure

import com.google.javascript.jscomp._
import sbt._
import scala.collection._

import com.samskivert.mustache.{Mustache,Template}

case class JsSource(val src: File, val des: File) extends Source {
  
  def isJsm = false
  
  lazy val imports: Seq[File] = Nil
  
  def closureSources: List[JSSourceFile] = 
    // TODO: Mustache
    //if(closureJsIsTemplated(path)) 
    //  JSSourceFile.fromCode(path.asFile.getAbsolutePath, renderTemplate(path))
    //else
    List(JSSourceFile.fromFile(src))
  
}

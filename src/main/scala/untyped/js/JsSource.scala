package untyped
package js

import com.google.javascript.jscomp._
import com.samskivert.mustache.{Mustache,Template}
import java.util.Properties
import sbt._
import scala.collection._

object JsSource {

  val requireRegex =
    """
    //[ \t]*require[ \t]*"([^"]+)"
    """.trim.r
  
  def parseRequire(line: String): Option[String] =
    requireRegex.findAllIn(line).matchData.map(data => data.group(1)).toList.headOption
  
  /**
   * By default the JMustache implementation will treat
   * variables named like.this as a two part name and look
   * for a variable called this within one called like
   * (called compound variables in the docs). This breaks
   * things with the default naming conventions for
   * Java/Lift properties so we turn it off.
   */
  lazy val mustacheCompiler =
    Mustache.compiler().standardsMode(true)
  
}

case class JsSource(val sources: Sources, val src: File, val des: File) extends Source {
  
  lazy val parents: List[Source] =
    for {
      line <- IO.readLines(src).map(_.trim).toList
      name <- JsSource.parseRequire(line)
    } yield sources.getSource(name, this)
  
  /** Closure sources for this file (not its parents). */
  def closureSources: List[JSSourceFile] =
    if(this.isTemplated) {
      List(JSSourceFile.fromCode(src.toString, renderTemplate))
    } else {
      List(JSSourceFile.fromFile(src))
    }
  
  override def requiresRecompilation: Boolean =
    if(isTemplated) {
      val props = new Props(sources.propertiesDir)
      
      super.requiresRecompilation || (props.file map (_ newerThan this.des) getOrElse false)
    } else {
      super.requiresRecompilation
    }
  
  /** Is the source file templated? It's templated if the file name contains ".template", e.g. "foo.template.js" */
  def isTemplated: Boolean =
    src.toString.contains(".template")
  
  /** The rendered template for this file. */
  def renderTemplate: String =
    JsSource.mustacheCompiler.compile(IO.read(src)).execute(attributes)

  /** Instantiate the properties used for Mustache templating */
  def attributes: Properties = {
    val props = new Props(sources.propertiesDir)
    props.properties.getOrElse {
      sources.log.warn("sbt-js: no properties file found in search path: " + props.searchPaths)
      new Properties
    }
  }
    
}

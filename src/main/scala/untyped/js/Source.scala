package untyped
package js

import com.google.javascript.jscomp
import com.google.javascript.jscomp._
import sbt._
import scala.collection._

trait Source {

  def sources: Sources
  def src: File
  def des: File

  lazy val srcDirectory: File =
    src.getParentFile

  /** Lines in the source file. */
  def lines: List[String] =
    IO.readLines(src).map(_.trim).toList
  
  /** Files that the source file depends on. */
  def parents: List[Source]
    
  def requiresRecompilation: Boolean =
    !des.exists ||
    (src newerThan des) ||
    parents.exists(_.requiresRecompilation)

  /** Compile the file and return the destination. */
  def compile: Option[File] = {
    sources.log.info("Compiling Javascript source %s".format(des))
    
    val compiler = new jscomp.Compiler
    
    jscomp.Compiler.setLoggingLevel(closureLogLevel)
    
    val myExterns = sources.closureExterns(this)
    val mySources = sources.closureSources(this)
    
    sources.log.debug("  externs:")
    myExterns.foreach(x => sources.log.debug("    " + x)) 

    sources.log.debug("  sources:")
    mySources.foreach(x => sources.log.debug("    " + x)) 
    
    val result =
      compiler.compile(
        myExterns.toArray,
        mySources.toArray,
        sources.compilerOptions)
    
    val errors = result.errors.toList
    val warnings = result.warnings.toList
    
    if(!errors.isEmpty) {
      sources.log.error(errors.length + " errors compiling " + src + ":")
      errors.foreach(err => sources.log.error(err.toString))
      
      None
    } else {
      if(!warnings.isEmpty) {
        sources.log.warn(warnings.length + " warnings compiling " + src + ":")
        warnings.foreach(err => sources.log.warn(err.toString))
      }
      
      IO.createDirectory(new File(des.getParent))
      IO.write(des, compiler.toSource)
      
      Some(des)
    }
  }
  
  /** Clean up the destination and any temporary files. */
  def clean: Unit = {
    sources.log.info("Cleaning Javascript source %s".format(des))
    IO.delete(des)
  }
  
  // Helpers ------------------------------------
  
  /** Closure externs for this file (not its parents). */
  def closureExterns: List[JSSourceFile] = Nil
  
  /** Closure sources for this file (not its parents). */
  def closureSources: List[JSSourceFile]
  
  def closureLogLevel: java.util.logging.Level =
    java.util.logging.Level.OFF
  
}

package untyped
package closure

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
  
  /** Compile the file and return the destination. */
  def compile(log: Logger, sources: Sources): Option[File] = {
    log.info("Compiling Javascript source %s".format(des))
    closureCompile(log)
  }
  
  /** Clean up the destination and any temporary files. */
  def clean(log: Logger): Unit = {
    log.info("Cleaning Javascript source %s".format(des))
    IO.delete(des)
  }
  
  // Helpers ------------------------------------
  
  /** Closure externs for this file (not its imports or parents). */
  def closureExterns: List[JSSourceFile] = Nil
  
  /** Closure sources for this file (not its imports or parents). */
  def closureSources: List[JSSourceFile]
  
  def closureLogLevel: java.util.logging.Level =
    java.util.logging.Level.OFF
  
  // TODO: Push to Plugin
  def closureOptions = {
    val options = new CompilerOptions
    options.variableRenaming = VariableRenamingPolicy.OFF
    options.prettyPrint = false
    options
  }

  def closureCompile(log: Logger): Option[File] = {
    val compiler = new jscomp.Compiler
    
    jscomp.Compiler.setLoggingLevel(closureLogLevel)
    
    val myExterns = sources.closureExterns(this)
    val mySources = sources.closureSources(this)
    
    log.debug("  externs:")
    myExterns.foreach(x => log.debug("    " + x)) 

    log.debug("  sources:")
    mySources.foreach(x => log.debug("    " + x)) 
    
    val result =
      compiler.compile(
        myExterns.toArray,
        mySources.toArray,
        closureOptions)
    
    val errors = result.errors.toList
    val warnings = result.warnings.toList
    
    if(!errors.isEmpty) {
      log.error(errors.length + " errors compiling " + src + ":")
      errors.foreach(err => log.error(err.toString))
      
      None
    } else {
      if(!warnings.isEmpty) {
        log.warn(warnings.length + " warnings compiling " + src + ":")
        warnings.foreach(err => log.warn(err.toString))
      }
      
      IO.createDirectory(new File(des.getParent))
      IO.write(des, compiler.toSource)
      
      Some(des)
    }
  }
  
}

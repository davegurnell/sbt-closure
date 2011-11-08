package untyped
package js

import com.google.javascript.jscomp._
import sbt._
import scala.collection._

case class Sources(
    val log: Logger,
    val sourceDir: File,
    val targetDir: File,
    val propertiesDir: File,
    val downloadDir: File,
    val compilerOptions: CompilerOptions
  ) {
  
  // Adding sources -----------------------------
  
  var sources: List[Source] = Nil
  
  def +=(file: File): Unit =
    this += createSource(file)

  def +=(url: URL): Unit =
    this += createSource(downloadAndCache(url))

  private def +=(source: Source): Unit =
    if(!sources.contains(source)) {
      sources = source :: sources
      source.parents.foreach(this += _)
    }
  
  def getSource(src: String, referredToBy: Source): Source =
    if(src.matches("^https?:.*")) {
      getSource(new URL(src))
    } else {
      getSource(new File(referredToBy.srcDirectory, src).getCanonicalFile)
    }
  
  def getSource(src: URL): Source =
    getSource(downloadAndCache(src))

  def getSource(src: File): Source =
    sources find (_.src == src) getOrElse createSource(src)

  private def createSource(src: File): Source =
    if(src.toString.trim.toLowerCase.endsWith(".jsm")) {
      JsmSource(this, src.getCanonicalFile, srcToDes(src).getCanonicalFile)
    } else {
      JsSource(this, src.getCanonicalFile, srcToDes(src).getCanonicalFile)
    }

  def srcToDes(file: File) = {
    println("sourceDir " + sourceDir)
    println("downloadDir " + downloadDir)
    println("file " + file)
    
    val des =
      IO.relativize(sourceDir, file) orElse
      IO.relativize(downloadDir, file) getOrElse
      (throw new Exception("Could not determine destination filename for " + file))
    
    new File(targetDir, des.replaceAll("[.]jsm(anifest)?$", ".js").replaceAll("[.]template", ""))
  }
  
  // Downloading and caching URLs ---------------
  
  def downloadAndCache(url: URL): File = {
    val file = downloadDir / url.toString.replaceAll("""[^-A-Za-z0-9.]""", "_")
    
    println("DOWNLOAD TO " + file)
    
    if(!file.exists) {
      val content = scala.io.Source.fromInputStream(url.openStream).mkString
      IO.createDirectory(downloadDir)
      IO.write(file, content)
    }
    
    file
  }
  
  // Reasoning about sources --------------------
  
  def sourcesRequiringRecompilation: List[Source] =
    sources filter (_.requiresRecompilation)
  
  def parents(a: Source): List[Source] =
    a.parents

  def children(a: Source): List[Source] =
    sources filter(b => b.parents.contains(a))
  
  def ancestors(a: Source): List[Source] =
    breadthFirstSearch(parents _, List(a), Nil).
    filterNot(_ == a)
    
  def descendents(a: Source): List[Source] =
    breadthFirstSearch(children _, List(a), Nil).
    filterNot(_ == a)
  
  def closureExterns(a: Source): List[JSSourceFile] =
    (a :: ancestors(a)).reverse.flatMap(_.closureExterns)

  def closureSources(a: Source): List[JSSourceFile] =
    (a :: ancestors(a)).reverse.flatMap(_.closureSources)

  def breadthFirstSearch(succ: (Source) => List[Source], open: List[Source], ans: List[Source]): List[Source] =
    open match {
      case Nil =>
        ans
      
      case next :: rest =>
        if(ans.contains(next)) {
          breadthFirstSearch(succ, rest, ans)
        } else {
          breadthFirstSearch(succ, rest ::: succ(next), next :: ans)
        }
    }

  def dump: Unit =
    sources.foreach { source =>
      log.debug("Javascript source:")
      
      log.debug("  src:")
      log.debug("    " + source.src)

      log.debug("  des:")
      log.debug("    " + source.des)

      log.debug("  templated?:")
      log.debug("    " + (source match {
                           case source: JsSource => source.isTemplated
                           case _                => false
                          }))
      
      log.debug("  recompile?:")
      log.debug("    " + source.requiresRecompilation)

      log.debug("  parents:")
      parents(source).foreach(src => log.debug("    " + src))
      
      log.debug("  children:")
      children(source).foreach(src => log.debug("    " + src))
      
      log.debug("  ancestors:")
      ancestors(source).foreach(src => log.debug("    " + src))
      
      log.debug("  descendents:")
      descendents(source).foreach(src => log.debug("    " + src))
    }
  
}
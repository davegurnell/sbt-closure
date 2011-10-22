package untyped
package closure

import sbt._
import scala.collection._

case class Sources(val sources: List[Source]) {
  
  def sourcesRequiringRecompilation: List[Source] =
    sources filter (requiresRecompilation _)
  
  def requiresRecompilation(a: Source): Boolean =
    !a.des.exists ||
    (a.src newerThan a.des) ||
    a.imports.exists(_ newerThan a.src) ||
    ancestors(a).exists(requiresRecompilation _)
  
  def parents(a: Source): List[Source] =
    sources filter(b => a.imports.contains(b.src))

  def children(a: Source): List[Source] =
    sources filter(b => b.imports.contains(a.src))
  
  def ancestors(a: Source): List[Source] =
    breadthFirstSearch(parents _, List(a), Nil).
    filterNot(_ == a)
    
  def descendents(a: Source): List[Source] =
    breadthFirstSearch(children _, List(a), Nil).
    filterNot(_ == a)
  
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

  def dump(log: Logger): Unit =
    sources.foreach { source =>
      log.debug("Javascript source:")
      
      log.debug("  src:")
      log.debug("    " + source.src)

      log.debug("  des:")
      log.debug("    " + source.des)
      
      log.debug("  recompile?:")
      log.debug("    " + requiresRecompilation(source))
      
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
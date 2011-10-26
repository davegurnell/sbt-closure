package untyped
package closure

import sbt._
import sbt.Keys._
import sbt.Project.Initialize

object Plugin extends sbt.Plugin {

  object JsKeys {
    val js = TaskKey[List[File]]("js", "Compile Javascript sources.")
    val jsSources = TaskKey[Sources]("js-sources", "List of Javascript sources.")
    val filter = SettingKey[FileFilter]("filter", "Filter for selecting Javascript sources from default directories.")
    val excludeFilter = SettingKey[FileFilter]("exclude-filter", "Filter for exclusing Javascript sources from default diretories.")
  }
  
  import JsKeys._
  
  def jsFileOf(file: File, sourceDir: File, targetDir: File) =
    Some(new File(targetDir, IO.relativize(sourceDir, file).get.replace(".jsm", ".js")))
  
  def jsSourcesTask: Initialize[Task[Sources]] =
    (streams, sourceDirectory in js, resourceManaged in js, filter in js, excludeFilter in js) map {
      (out, sourceDir, targetDir, filter, excludeFilter) =>
        val sources =
          for {
            src <- sourceDir.descendentsExcept(filter, excludeFilter).get
            des <- jsFileOf(src, sourceDir, targetDir)
          } yield Source(src, des)
        
        Sources(sources.toList)
    }
  
  def jsCompilerTask =
    (streams, jsSources in js) map {
      (out, jsSources: Sources) =>
        jsSources.dump(out.log)
        
        jsSources.sourcesRequiringRecompilation match {
          case Nil =>
            out.log.info("No Javascript sources requiring compilation")
            Nil
          
          case toCompile =>
            toCompile.flatMap(_.compile(out.log, jsSources))
        }
    }
  
  def jsCleanTask =
    (streams, jsSources in js) map {
      (out, jsSources) =>
        for {
          source <- jsSources.sources
        } source.clean(out.log)
    }

  def jsSettingsIn(conf: Configuration): Seq[Setting[_]] =
    inConfig(conf)(Seq(
      filter in js := "*.js" | "*.jsm",
      excludeFilter in js := (".*" - ".") || HiddenFileFilter,
      sourceDirectory in js <<= (sourceDirectory in conf),
      resourceManaged in js <<= (resourceManaged in conf),
      jsSources in js <<= jsSourcesTask,
      clean in js <<= jsCleanTask,
      js <<= jsCompilerTask))
  
  def jsSettings: Seq[Setting[_]] =
    jsSettingsIn(Compile) ++
    jsSettingsIn(Test)
    
}

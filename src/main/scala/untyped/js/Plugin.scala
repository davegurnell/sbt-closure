package untyped
package js

import java.nio.charset.Charset
import sbt._
import sbt.Keys._
import sbt.Project.Initialize

object Plugin extends sbt.Plugin {

  object JsKeys {
    lazy val js = TaskKey[List[File]]("js", "Compile Javascript sources and manifest files")
    lazy val jsSources = TaskKey[Sources]("js-sources", "List of Javascript sources and manifest files")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in Javascript file IO (default utf-8)")
    lazy val propertiesDirectory = SettingKey[File]("properties-dir", "Directory containing properties for use in templated Javascript sources")
    lazy val downloadDirectory = SettingKey[File]("download-dir", "Temporary directory to download Javascript files to")
  }
  
  import JsKeys._
  
  def jsSourceFilesTask: Initialize[Task[Seq[File]]] =
    (streams, jsSources in js) map {
      (out, sources) => sources.sources.map(_.src)
    }
  
  def jsSourcesTask: Initialize[Task[Sources]] =
    (streams, sourceDirectory in js, resourceManaged in js, includeFilter in js, excludeFilter in js, propertiesDirectory in js, downloadDirectory in js) map {
      (out, sourceDir, targetDir, includeFilter, excludeFilter, propertiesDir, downloadDir) =>
        val sources = Sources(
          log           = out.log,
          sourceDir     = sourceDir, 
          targetDir     = targetDir,
          propertiesDir = propertiesDir,
          downloadDir   = downloadDir
        )
        
        for {
          src <- sourceDir.descendentsExcept(includeFilter, excludeFilter).get
        } sources += src
      
        sources
    }
  
  def jsCompilerTask =
    (streams, jsSources in js) map {
      (out, jsSources: Sources) =>
        jsSources.dump
        
        jsSources.sourcesRequiringRecompilation match {
          case Nil =>
            out.log.info("No Javascript sources requiring compilation")
            Nil
          
          case toCompile =>
            toCompile.flatMap(_.compile)
        }
    }
  
  def jsCleanTask =
    (streams, jsSources in js) map {
      (out, jsSources) =>
        jsSources.sources.foreach(_.clean)
    }

  def jsSettingsIn(conf: Configuration): Seq[Setting[_]] =
    inConfig(conf)(Seq(
      charset in js             :=  Charset.forName("utf-8"),
      includeFilter in js       :=  "*.js" || "*.jsm" || "*.jsmanifest",
      excludeFilter in js       :=  (".*" - ".") || HiddenFileFilter,
      sourceDirectory in js     <<= (sourceDirectory in conf),
      resourceManaged in js     <<= (resourceManaged in conf),
      propertiesDirectory in js <<= (resourceDirectory in conf),
      downloadDirectory in js   <<= (target in conf) { _ / "sbt-js" / "downloads" },
      jsSources in js           <<= jsSourcesTask,
      unmanagedSources in js    <<= jsSourceFilesTask,
      clean in js               <<= jsCleanTask,
      js                        <<= jsCompilerTask
    )) ++
    inConfig(conf)(Seq(
      cleanFiles   <+=  resourceManaged in js,
      watchSources <++= unmanagedSources in js
    ))
  
  def jsSettings: Seq[Setting[_]] =
    jsSettingsIn(Compile) ++
    jsSettingsIn(Test)
    
}

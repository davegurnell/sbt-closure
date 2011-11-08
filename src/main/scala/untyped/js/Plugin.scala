package untyped
package js

import com.google.javascript.jscomp._
import java.nio.charset.Charset
import sbt._
import sbt.Keys._
import sbt.Project.Initialize

object Plugin extends sbt.Plugin {

  object JsKeys {
    lazy val js                     = TaskKey[List[File]]("js", "Compile Javascript sources and manifest files")
    lazy val jsSources              = TaskKey[Sources]("js-sources", "List of Javascript sources and manifest files")
    lazy val charset                = SettingKey[Charset]("charset", "Sets the character encoding used in Javascript file IO (default utf-8)")
    lazy val propertiesDirectory    = SettingKey[File]("properties-dir", "Directory containing properties for use in templated Javascript sources")
    lazy val downloadDirectory      = SettingKey[File]("download-dir", "Temporary directory to download Javascript files to")
    lazy val variableRenamingPolicy = SettingKey[VariableRenamingPolicy]("js-variable-renaming-policy", "Options for the Google Closure compiler")
    lazy val prettyPrint            = SettingKey[Boolean]("js-pretty-print", "Options for the Google Closure compiler")
    lazy val compilerOptions        = SettingKey[CompilerOptions]("js-compiler-options", "Options for the Google Closure compiler")
  }
  
  /** Provides quick access to the enum values in com.google.javascript.jscomp.VariableRenamingPolicy */
  object VariableRenamingPolicy {
    val ALL         = com.google.javascript.jscomp.VariableRenamingPolicy.ALL
    val LOCAL       = com.google.javascript.jscomp.VariableRenamingPolicy.LOCAL
    val OFF         = com.google.javascript.jscomp.VariableRenamingPolicy.OFF
    val UNSPECIFIED = com.google.javascript.jscomp.VariableRenamingPolicy.UNSPECIFIED
  }
  
  import JsKeys._
  
  def jsSourceFilesTask: Initialize[Task[Seq[File]]] =
    (streams, jsSources in js) map {
      (out, sources) => sources.sources.map(_.src)
    }
  
  def jsSourcesTask: Initialize[Task[Sources]] =
    (streams, sourceDirectory in js,resourceManaged in js, includeFilter in js, excludeFilter in js, propertiesDirectory in js, downloadDirectory in js, compilerOptions in js) map {
      (out, sourceDir, targetDir, includeFilter, excludeFilter, propertiesDir, downloadDir, compilerOptions) =>
        val sources = Sources(
          log            = out.log,
          sourceDir      = sourceDir, 
          targetDir      = targetDir,
          propertiesDir  = propertiesDir,
          downloadDir    = downloadDir,
          compilerOptions = compilerOptions
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
  
  def compilerOptionsSetting: Initialize[CompilerOptions] =
    (streams, variableRenamingPolicy in js, prettyPrint in js) apply {
      (out, variableRenamingPolicy, prettyPrint) =>
        val options = new CompilerOptions
        options.variableRenaming = variableRenamingPolicy
        options.prettyPrint = prettyPrint
        options
    }

  def jsSettingsIn(conf: Configuration): Seq[Setting[_]] =
    inConfig(conf)(Seq(
      charset in js                :=  Charset.forName("utf-8"),
      includeFilter in js          :=  "*.js" || "*.jsm" || "*.jsmanifest",
      excludeFilter in js          :=  (".*" - ".") || HiddenFileFilter,
      sourceDirectory in js        <<= (sourceDirectory in conf),
      resourceManaged in js        <<= (resourceManaged in conf),
      propertiesDirectory in js    <<= (resourceDirectory in conf),
      downloadDirectory in js      <<= (target in conf) { _ / "sbt-js" / "downloads" },
      jsSources in js              <<= jsSourcesTask,
      unmanagedSources in js       <<= jsSourceFilesTask,
      variableRenamingPolicy in js :=  VariableRenamingPolicy.LOCAL,
      prettyPrint in js            :=  false,
      compilerOptions in js        <<= compilerOptionsSetting,
      clean in js                  <<= jsCleanTask,
      js                           <<= jsCompilerTask
    )) ++
    inConfig(conf)(Seq(
      cleanFiles   <+=  resourceManaged in js,
      watchSources <++= unmanagedSources in js
    ))
  
  def jsSettings: Seq[Setting[_]] =
    jsSettingsIn(Compile) ++
    jsSettingsIn(Test)
    
}

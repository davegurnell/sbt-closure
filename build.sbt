name := "sbt-closure"

version := "0.6-SNAPSHOT"

organization := "untyped"

scalaVersion := "2.9.1"

sbtPlugin := true

resolvers += "Untyped Public Repo" at "http://repo.untyped.com"

libraryDependencies ++= Seq(
  "com.google.javascript" % "closure-compiler" % "r706",
  "com.samskivert" % "jmustache" % "1.3",
  "org.scalatest" % "scalatest" % "1.1"
)

seq(ScriptedPlugin.scriptedSettings: _*)

// Make the scripted SBT plugin print things immediately.
// Useful when inserting pause statements into the test scripts:
scriptedBufferLog := false

publishTo := {
  val host = System.getenv("DEFAULT_REPO_HOST")
  val path = System.getenv("DEFAULT_REPO_PATH")
  val user = System.getenv("DEFAULT_REPO_USER")
  val keyfile = new File(System.getenv("DEFAULT_REPO_KEYFILE"))
  Some(Resolver.sftp("Default Repo", host, path).as(user, keyfile))
}

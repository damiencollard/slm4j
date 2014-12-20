name := "sts"

version := "1.2-SNAPSHOT"

organization := "org.distfp"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.specs2"         %% "specs2-core"        % "2.4.11"        % "test"
)

TaskKey[Seq[File]]("mksh") <<= (baseDirectory, fullClasspath in Runtime) map { (base, cp) =>
  val tools = Map(
    "sts.sh" -> "org.distfp.sts.StsTool"
  )
  tools.keys.toList map { scriptName =>
    makeShellScript(base, cp, tools(scriptName), scriptName)
  }
}

def makeShellScript(baseDir: File, classPath: Seq[Attributed[File]], className: String, scriptName: String): File = {
  val template = """#!/bin/sh
                   |java -classpath "%s:${CLASSPATH}" %s "$@"
                 """.stripMargin
  println(s"Making shell script '$scriptName' for $className")
  val contents = template.format(classPath.files.absString, className)
  val out = baseDir / ("bin/" + scriptName)
  IO.write(out, contents)
  out.setExecutable(true)
  out
}

// Remove the _<scalaVersion> suffix from artifacts.
crossPaths := false

// Publish artifacts to Ubeeko's Nexus.
publishMavenStyle in ThisBuild := true

publishTo in ThisBuild := {
  val nexus = "http://intra.ubeeko.lan/nexus/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots/")
  else
    Some("releases"  at nexus + "content/repositories/releases/")
}


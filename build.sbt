name := "slm4j"

version := "1.0"

organization := "damiencollard"

TaskKey[Seq[File]]("mksh") <<= (baseDirectory, fullClasspath in Runtime) map { (base, cp) =>
  val tools = Map(
    "slm4j.sh" -> "starschema.slm4j.Main"
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


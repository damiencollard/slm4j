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

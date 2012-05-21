package com.sake.build

import java.io.File
object CompileHelper {

  lazy val compilerPath = try {
    jarPathOfClass("scala.tools.nsc.Interpreter")
  } catch {
    case e =>
      throw new RuntimeException("Unable lo load scala interpreter from classpath (scala-compiler jar is missing?)", e)
  }

  lazy val javaCompilerPath = try {
    val jarPathJavaC = jarPathOfClass("com.sun.tools.javac.Main")
    println("Classpath to javac "+jarPathJavaC.mkString)
    jarPathJavaC
  } catch {
    case e =>
      e.printStackTrace()
      throw new RuntimeException("Unable lo load javac from classpath (tools jar is missing?)", e)
  }
  lazy val sakePath =
    try {
      jarPathOfClass("com.sake.build.Sake")
    } catch {
      case e => Nil
//        throw new RuntimeException("Unable lo load Sake from classpath (sake jar is missing?)", e)
    }

  lazy val libPath = try {
    jarPathOfClass("scala.ScalaObject")
  } catch {
    case e =>
      throw new RuntimeException("Unable to load scala base object from classpath (scala-library jar is missing?)", e)
  }
  
  private def jarPathOfClass(className: String) = try {
    val resource = className.split('.').mkString("/", "/", ".class")
    val path = getClass.getResource(resource).getPath
    val indexOfFile = path.indexOf("file:") + 5
    val indexOfSeparator = path.lastIndexOf('!')
    List(path.substring(indexOfFile, indexOfSeparator))
  }

  def scalaFilesInDirectory(dir: String) = {
    new File(dir)
  }
}
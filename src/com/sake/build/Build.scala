package com.sake.build

import java.io.File
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Jar
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import util.FileListing
import com.sake.build.ivy.JarDependency

/**
 * @author soren
 *
 */
trait Build {

  var rootPath = "."
  var classpath = ""
  def sourceFolders: List[String] = List("src")
  def testFolders: List[String] = List("src_test")

  def projectRoot(rootPath: String) = this.rootPath = rootPath
  def classpath(classpath: String) { this.classpath = classpath }
  def preCompile {}

  def compile {
    println("******************************************************")
    println("* Compiling " + projectName + " in " + rootPath)
    println("* Classpath " + classpath.mkString)
    println("******************************************************")
    println("Compiling files in " + sourceFolders.mkString(","))
    try {

      val settings = new Settings
      settings.deprecation.value = true // enable detailed deprecation warnings
      settings.unchecked.value = true // enable detailed unchecked warnings
      settings.outputDirs.setSingleOutput(classOutputDirectory)

      val pathList = /*List("bin") :::*/ CompileHelper.compilerPath ::: CompileHelper.libPath
      settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
      settings.classpath.value = classpath.mkString + File.pathSeparatorChar + jarDependencies.map(f => f.getJarFile.getAbsolutePath()).mkString(""+File.pathSeparatorChar)

      val reporter = new ConsoleReporter(settings)
      val compiler = new Global(settings, reporter)
      val filesToCompile = sourceFolders.flatMap(folder => FileListing.getFileListing(new File(rootPath, folder), f => {
        f.getAbsoluteFile().toString().endsWith(".scala")
      }).map(f => f.getAbsolutePath()))

      println("Trying to compile the following files: " + filesToCompile.mkString("\n"))
      (new compiler.Run).compile(filesToCompile)
      compiler.currentRun.cancel()
      println("Done...")
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

  def postCompile {}

  /**
   * Creates a jar file from the target directory
   *
   * @return jarfile
   */
  def packageJar = {
    println("Creating Jar '" + jarName + "' in directory " + jarOutputDirectory)
    val jarFile = Jar.create(scala.tools.nsc.io.File(jarName), Directory(jarOutputDirectory), mainClass)
    jarFile
  }

  def jarName = { jarOutputDirectory + File.separatorChar + projectName + ".jar" }

  def jarOutputDirectory = {
    val dir = rootPath + File.separatorChar + outputDirectory + File.separatorChar + "jars"
    new File(dir).mkdirs()
    dir
  }

  def projectName: String

  def mainClass = ""

  def classOutputDirectory = {
    val dir = rootPath + File.separatorChar + outputDirectory + File.separatorChar + "classes"
    new File(dir).mkdirs()
    dir
  }

  def outputDirectory = "target"

  /**
   * Defines list of sub projects the this build depends on
   */
  def subProjects: List[SubProject] = Nil
  
  def jarDependencies: List[JarDependency] = Nil
}
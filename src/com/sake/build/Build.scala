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
import java.io.PrintWriter
import java.io.StringWriter
import com.sun.tools.javac._
import com.sake.build.ivy.IvyResolver$
import com.sake.build.ivy.IvyResolver

/**
 * @author soren
 *
 */
trait Build {

  var rootPath = "."
  var classpath = ""
  def sourceFolders: List[String] = List("src")
  def testFolders: List[String] = List("src_test")

  def preCompile {}

  def compile {
    try {

      val settings = new Settings
      settings.deprecation.value = true // enable detailed deprecation warnings
      settings.unchecked.value = true // enable detailed unchecked warnings
      settings.outputDirs.setSingleOutput(classOutputDirectory)

      val pathList = CompileHelper.compilerPath ::: CompileHelper.libPath
      settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
      val ivyXMLDepends = IvyResolver.resolveIvyXML("ivy.xml")
      println("Deps from ivy xml: "+ivyXMLDepends.mkString(File.pathSeparator))
      settings.classpath.value = ivyXMLDepends.mkString(File.pathSeparator) + classpath.mkString + File.pathSeparatorChar + jarDependencies.map(f => f.getJarFile.getCanonicalPath()).mkString(File.pathSeparator)
      println("\n\n")
      println("**********************************************************************************************************************")
      println("* Compiling " + projectName + " in " + rootPath)
      println("* Classpath " + settings.classpath.value)
      println("* Compiling files in " + sourceFolders.mkString(","))
      println("* Build root path = " + rootPath)
      println("**********************************************************************************************************************")

      val scalaFilesToCompile = sourceFolders.flatMap(folder => FileListing.getFileListing(new File(rootPath, folder), f => {
        f.getAbsoluteFile().toString().endsWith(".scala")
      }).map(f => f.getCanonicalPath()))

      val javaFilesToCompile = sourceFolders.flatMap(folder => FileListing.getFileListing(new File(rootPath, folder), f => {
        f.getAbsoluteFile().toString().endsWith(".java")
      }).map(f => f.getCanonicalPath()))

      if (!javaFilesToCompile.isEmpty) {

        // java compile
        println("Compiling java")
        val writer = new PrintWriter("/tmp/test.txt")
        val sourcePath = sourceFolders.map(sf => new File(rootPath, sf).getCanonicalPath()).mkString("" + File.pathSeparatorChar)
        println("SourcePath: " + sourcePath)
        val result = exec((List("-cp", settings.classpath.value, "-d", rootPath + File.separator + "target/classes", "-sourcepath", sourcePath) ::: javaFilesToCompile).toArray, writer)
        if (result != 0)
          throw new RuntimeException("Error compiling java code")
        println("Done compiling java with result " + result)
      }

      // scala compile
      if (!scalaFilesToCompile.isEmpty) {
        println("Trying to compile the following files: " + scalaFilesToCompile.mkString("\n"))
        println("Compiling scala")
        val reporter = new ConsoleReporter(settings)
        val compiler = new Global(settings, reporter)
        val runner = (new compiler.Run)
        runner.compile(scalaFilesToCompile)
        compiler.currentRun.cancel()
        println("Done...")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

  def exec(args: Array[String], writer: PrintWriter) = {
    Main.compile(args).intValue()
  }

  def postCompile {}

  /**
   * Creates a jar file from the target directory
   *
   * @return jarfile
   */
  def packageJar = {
    println("Creating Jar '" + jarName + "' in directory " + jarOutputDirectory)
    val jarFile = Jar.create(scala.tools.nsc.io.File(jarName), Directory(outputDirectory + File.separatorChar + "classes"), mainClass)
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
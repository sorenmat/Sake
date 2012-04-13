package com.sake.build

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.Results
import scala.tools.nsc.Settings
import com.sake.build.ivy.IvyResolver
import com.sake.build.ivy.JarDependency

object Sake extends App {

  override def main(args: Array[String]) {
    if (args.size == 0) {
      println("Sake")
      println("You need to specify atleast one of the following parameters")
      println("* compile - Compiles the source files.")
      println("* packageJar - Creates a jar file from the compiled classes.")

    }
    areWeInProjectRoot
    super.main(args)
    val path = if (args.size > 1) args(1) else "."
    Sake.runTargetOnBuild(args(0), path)
  }

  def runTargetOnBuild(target: String, rootPath: String) {
    try {

      val settings = new Settings
      settings.deprecation.value = true // enable detailed deprecation warnings
      settings.unchecked.value = true // enable detailed unchecked warnings

      new File(rootPath, "target").mkdirs() // ensure that target directory exists
      settings.outputDirs.setSingleOutput("target")

      // bin is a eclipse hack
      val pathList = List("/Users/soren/git/Sake/lib/tools.jar", IvyResolver.resolve(new JarDependency("org.apache.ivy", "ivy", "2.2.0")).getAbsolutePath()) ::: CompileHelper.sakePath ::: CompileHelper.compilerPath ::: CompileHelper.libPath
      settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
      settings.classpath.value = pathList.mkString(File.pathSeparator)

      val buildFile = findBuildFile(rootPath + File.separatorChar + "project").get
      println("Found build file '" + buildFile + "'")
      val projectFile = Source.fromFile(buildFile).mkString

      val bout = new ByteArrayOutputStream()
      val interp = new IMain(settings, new PrintWriter(bout))

      val result = interp.interpret(projectFile) // "compile" project file
      result match {
        case Results.Error => println("Error Build compile message: " + String.valueOf(bout))
        case Results.Success => {
          val output = String.valueOf(bout)
          val className = output.substring(output.indexOf("defined class") + ("defined class").size, output.length()).trim()
          bout.reset()
          val subProjectResult = interp.interpret("new " + className + "().subProjects.map(sp => sp.path).mkString(\",\")")
          val subProjects = String.valueOf(bout)

          if (!subProjects.isEmpty()) {

            val projects = subProjects.split("=")(1).trim()
            if (projects != null && !projects.isEmpty() && !"\"\"".equals(projects)) {
              // compile sub projects
              println("This project needs to compile the following projects first: " + projects)
              val projectsPaths = projects.split(",")
              projectsPaths.foreach(projectPath => {

                println("Settings projectPath in runTargetOnBuild to " + new File(".", projectPath).getCanonicalPath())
                runTargetOnBuild(target, new File(".", projectPath).getCanonicalPath())
                settings.classpath.value = settings.classpath.value + File.pathSeparator + new File(projectPath + File.separatorChar + "target" + File.separatorChar + "classes").getCanonicalPath()
                //                subProjectsClassPath = subProjectsClassPath + settings.classpath.value 
              })
            }
          }
          val classpath = settings.classpath.value + File.pathSeparator + jarCache.values.mkString
          executeBuild(interp, bout, className, rootPath, classpath, target)

          println("Result: " + result.toString())
          println("\nCompile done.")
        }
        case Results.Incomplete =>
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

  /**
   * Execute the build file. The interp assumes the build file as been loaded
   */
  def executeBuild(interp: IMain, bout: ByteArrayOutputStream, className: String, rootPath: String, classpath: String, target: String) = {
    interp.reporter.withoutTruncating {
      bout.reset()
      val varName = className.toLowerCase()

      interp.interpret("val " + varName + " = new " + className + "()")
      
//      println("[DEBUG] "+"" + varName + ".rootPath = \"" + rootPath.trim()+ "\"")
      interp.interpret("" + varName + ".rootPath = \"" + rootPath.trim()+ "\"")
      
//      println("[DEBUG] "+"" + varName + ".classpath = \"" + classpath.trim() + "\"")
      interp.interpret("" + varName + ".classpath = \"" + classpath.trim() + "\"")
      
      
      println("\n" + String.valueOf(bout) + "\n")
      interp.interpret("" + varName + "." + target)
      println("\nCompiled project file resultet in: " + String.valueOf(bout) + "\n")
      bout.reset()
      interp.interpret("" + varName + ".jarDependencies.map(f => f.getJarFile.getCanonicalPath()).mkString(\",\")")
      val subprojectDependenciesTmp = String.valueOf(bout)
      val subprojectDependencies = subprojectDependenciesTmp.substring(subprojectDependenciesTmp.indexOf(" = ") + 3)
      jarCache += className -> subprojectDependencies.split(",").mkString(File.pathSeparator)
    }
  }
  /**
   * List of jars that subprojects use
   */
  var jarCache: Map[String, String] = Map()

  def findBuildFile(projectPath: String) = {
    val files = new File(projectPath).listFiles()
    if (files == null) {
      throw new RuntimeException("Unable to find project files in " + projectPath + " relative to " + new File(".").getAbsolutePath())

    }
    files.find(f => f.getAbsolutePath().endsWith(".scala"))
  }

  def areWeInProjectRoot {
    if (!new File(".", "project").exists())
      throw new RuntimeException("You need to be in a project root when executing sake")
  }
}


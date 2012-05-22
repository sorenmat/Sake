package com.sake.build

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.Results
import scala.tools.nsc.interpreter.Results._
import scala.tools.nsc.Settings
import com.sake.build.ivy.IvyResolver
import com.sake.build.ivy.JarDependency
import com.sake.build.util.StringUtils._

object Sake extends App {

  case class Task(name: String)
  case class Project(path: String)

  /**
   * Map of what task as been completed on what projects
   */
  var taskCache: List[(Project, Task)] = Nil

  override def main(args: Array[String]) {
    if (args.size == 0) {
      println("Sake")
      println("You need to specify atleast one of the following parameters")
      println("* compile - Compiles the source files.")
      println("* packageJar - Creates a jar file from the compiled classes.")

    } else {
      areWeInProjectRoot
      super.main(args)
      val path = if (args.size > 1) args(1) else "."
      Sake.runTargetOnBuild(args(0), path)
    }
  }

  /**
   *
   * @return Projetname, List of exported jar dependencies.
   */
  def runTargetOnBuild(target: String, rootPath: String) {
    try {
      val targetKey = (Project(rootPath), Task(target))
      if (!taskCache.contains(targetKey)) {
        val settings = new Settings
        settings.deprecation.value = true // enable detailed deprecation warnings
        settings.unchecked.value = true // enable detailed unchecked warnings

        new File(rootPath, "target").mkdirs() // ensure that target directory exists
        settings.outputDirs.setSingleOutput("target")

        // bin is a eclipse hack
        val pathList = List("bin", IvyResolver.resolve(new JarDependency("org.apache.ivy", "ivy", "2.2.0")).get.getAbsolutePath()) ::: CompileHelper.sakePath ::: CompileHelper.compilerPath ::: CompileHelper.javaCompilerPath ::: CompileHelper.libPath
        settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
        settings.classpath.value = pathList.mkString(File.pathSeparator)

        val buildFile = findBuildFile(rootPath + File.separatorChar + "project").get
        println("Found build file '" + buildFile + "'")
        val projectFile = Source.fromFile(buildFile).mkString

        val bout = new ByteArrayOutputStream()
        val start = System.currentTimeMillis()
        val interp = new IMain(settings, new PrintWriter(bout))

        val result = interp.interpret(projectFile) // "compile" project file
        val stop = System.currentTimeMillis()
        printf("Interpetation of build file took %s ms.\n", (stop - start))
        result match {
          case Results.Error => throw new RuntimeException("Error Build compile message: " + String.valueOf(bout))
          case Results.Success => {
            val output = String.valueOf(bout)
            val className = output.substring(output.indexOf("defined class") + ("defined class").size, output.length()).trim()
            bout.reset()
            executeLine(interp, "val " + className.toLowerCase() + " = new " + className + "()")
            executeLine(interp, "" + className.toLowerCase() + ".rootPath = \"" + rootPath.trim() + "\"")
            bout.reset()
            val subProjectResult = interp.interpret(className.toLowerCase() + ".subProjects.map(sp => sp.path).mkString(\",\")")
            val subProjects = String.valueOf(bout)

            var buildedSubProjects: List[String] = Nil
            if (!subProjects.isEmpty()) {

              val projects = subProjects.split("=")(1).trim()
              if (projects != null && !projects.isEmpty() && !"\"\"".equals(projects)) {
                println(className + " has the following subprojects " + projects)
                // compile sub projects
                val projectsPaths = projects.split(",")
                projectsPaths.foreach(projectPath => {

                  println("Settings projectPath in runTargetOnBuild to " + new File(projectPath).getCanonicalPath())
                  runTargetOnBuild(target, new File(projectPath).getCanonicalPath())
                  settings.classpath.value = settings.classpath.value + File.pathSeparator + projectsCompiledClasses(projectPath)
                  debug("Returned ClassPath: " + settings.classpath.value)
                  println("\n**\n**\n" + className + "->" + new File(projectPath).getCanonicalPath() + "/target/classes" + "\n")
                  println(settings.classpath.value + "\n\n\n\n\n")
                  buildedSubProjects = className :: buildedSubProjects
                })
              }
            }
            executeBuild(interp, bout, className, rootPath, settings.classpath.value, target)
            taskCache = targetKey :: taskCache // add target to taskCache

            println("Result: " + result.toString())
            println("\nCompile done.")
          }
          case Results.Incomplete =>
        }
      } else {
        println("Task '" + targetKey + "'already executed")
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
      debug("executeBuild classpath: " + trim(classpath))
      executeLine(interp, "" + varName + ".classpath = \"" + trim(classpath) + "\"")
      executeLine(interp, "" + varName + "." + target)
      bout.reset()
    }
  }

  def executeLine(interp: IMain, line: String) {
    interp.interpret(line) match {
      case Success =>
      case Incomplete => throw new RuntimeException("Incomplete")
      case Error => throw new RuntimeException("Error in line: " + line)
    }
  }

  def projectsCompiledClasses(projectPath: String) = {
    debug("projectsCompiledClasses = " + new File(projectPath + File.separatorChar + "target" + File.separatorChar + "classes").getCanonicalPath())
    new File(projectPath + File.separatorChar + "target" + File.separatorChar + "classes").getCanonicalPath()
  }

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

  def debug(msg: String) {
    println("[DEBUG] - " + msg)
  }

  def addToMap(map: Map[String, String], key: String, newData: String) = {
    val data = map.get(key).get
    val newMap = map + key -> (data + newData)
    newMap
  }

}


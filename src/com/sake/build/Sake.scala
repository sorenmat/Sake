package com.sake.build

import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.Results
import scala.tools.nsc.interpreter.Results._
import scala.tools.nsc.Settings
import com.sake.build.ivy.IvyResolver
import com.sake.build.log._
import com.sake.build.ivy.JarDependency
import com.sake.build.util.StringUtils._


object Sake extends App with Logger {

  case class Task(name: String)
  case class Project(path: String)

  /**
   * Map of what task as been completed on what projects
   */
  var taskCache: List[(Project, Task)] = Nil

  override def main(args: Array[String]) {
    println(header)
    if (args.size == 0) {
      info("Sake")
      info("You need to specify atleast one of the following parameters")
      info("* compile - Compiles the source files.")
      info("* packageJar - Creates a jar file from the compiled classes.")

    } else {
      areWeInProjectRoot
      super.main(args)
      Sake.runTargetOnBuild(args.toList, ".")
    }
  }

  /**
   *
   * @return Projetname, List of exported jar dependencies.
   */
  def runTargetOnBuild(targets: List[String], rootPath: String) {

    try {
      //val targetKey = (Project(rootPath), Task(target))
      //if (taskCache != null && !taskCache.contains(targetKey)) {
          val settings = new Settings
          settings.deprecation.value = true // enable detailed deprecation warnings
          settings.unchecked.value = true // enable detailed unchecked warnings
          //settings.embeddedDefaults(this.getClass.getClassLoader)
          new File(rootPath, "target").mkdirs() // ensure that target directory exists
          settings.outputDirs.setSingleOutput("target")

          // bin is a eclipse hack


          val resolver = new IvyResolver()
          //TODO scan some plugin directory for plugin dependencies
          val pathList = List("bin", resolver.resolve(new JarDependency("com.beust", "jcommander", "1.26")).get.getAbsolutePath(), resolver.resolve(new JarDependency("org.testng", "testng", "6.5.2")).get.getAbsolutePath(), resolver.resolve(new JarDependency("org.apache.ivy", "ivy", "2.2.0")).get.getAbsolutePath()) ::: CompileHelper.sakePath ::: CompileHelper.compilerPath ::: CompileHelper.javaCompilerPath ::: CompileHelper.libPath

          settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
          settings.classpath.value = pathList.mkString(File.pathSeparator)
          debug("Sake classpath "+settings.classpath.value)

          val buildFile = findBuildFile(rootPath + File.separatorChar + "sake").get
          info("Found build file '" + buildFile + "'")
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

              // create a new instance of the build in the interpreter
              executeLine(interp, "val " + className.toLowerCase() + " = new " + className + "()", bout)

              val tasks = getTasks(bout, interp, className)
              println("Please run one of the following tasks: ")
              tasks.foreach(task => println(task._1))
               
              executeLine(interp, "" + className.toLowerCase() + ".rootPath = \"" + rootPath.trim() + "\"", bout)
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

                    info("Settings projectPath in runTargetOnBuild to " + new File(projectPath).getCanonicalPath())

                    runTargetOnBuild(targets, new File(projectPath).getCanonicalPath())
                    settings.classpath.value = settings.classpath.value + File.pathSeparator + projectsCompiledClasses(projectPath)
                    debug("Returned ClassPath: " + settings.classpath.value)
                    info("\n**\n**\n" + className + "->" + new File(projectPath).getCanonicalPath() + "/target/classes" + "\n")
                    info(settings.classpath.value + "\n\n\n\n\n")
                    buildedSubProjects = className :: buildedSubProjects
                  })
                }
              }
              targets.foreach(currenttarget => {
                try {
                executeBuild(interp, bout, className, rootPath, settings.classpath.value, currenttarget)

                } catch {
                  case e: RuntimeException => info("Execution of target "+currenttarget+" went wrong"); e.printStackTrace()
                }
              })
              //executeBuild(interp, bout, className, rootPath, settings.classpath.value, target)
              //taskCache = targetKey :: taskCache // add target to taskCache
              //println("Result: " + result.toString())
              //println("\nCompile done.")
            }
            case Results.Incomplete =>
          }
        //} else {
        //println("Task '" + targetKey + "'already executed")
      //}

    } catch {
      case e: Throwable => e.printStackTrace()
    }

  }

  /**
   * Execute the build file. The interp assumes the build file as been loaded
   */
  def executeBuild(interp: IMain, bout: ByteArrayOutputStream, className: String, rootPath: String, classpath: String, target: String) = {
      val varName = className.toLowerCase()
      /*val build = interp.valueOfTerm(varName)
      build match {
        case Some(x) => {
          val b = x.asInstanceOf[Build]
          println("\n\n\n\n\n"+b.projectName+"\n\n\n\n\n")
        }
        case None =>
      }
      */
    interp.reporter.withoutTruncating {
      bout.reset()
      debug("executeBuild classpath: " + trim(classpath))
//      executeLine(interp, "" + varName + ".classpath = \"" + trim(classpath) + "\"")
      executeLine(interp, "" + varName + "." + target, bout)
      bout.reset()

    }
  }

  def executeLine(interp: IMain, line: String,bout: ByteArrayOutputStream) {
    interp.interpret(line) match {
      case Success =>
      case Incomplete => throw new RuntimeException("Incomplete")
      case Error => throw new RuntimeException("Error in line: " + line+"\n\n"+String.valueOf(bout))
    }
  }

  def projectsCompiledClasses(projectPath: String) = {
    debug("projectsCompiledClasses = " + new File(projectPath + File.separatorChar + "target" + File.separatorChar + "classes").getCanonicalPath())
    new File(projectPath + File.separatorChar + "target" + File.separatorChar + "classes").getCanonicalPath()
  }

  def findBuildFile(projectPath: String) = {
    val path = new File(projectPath)
    debug(path.getCanonicalPath)
    val files = path.listFiles()
    if (files == null) {
      throw new RuntimeException("Unable to find project files in " + projectPath + " relative to " + new File(".").getAbsolutePath())

    }
    files.find(f => f.getAbsolutePath().endsWith(".scala"))
  }

  def areWeInProjectRoot {
    if (!new File(".", "sake").exists())
      throw new RuntimeException("You need to be in a project root when executing sake")
  }

  def addToMap(map: Map[String, String], key: String, newData: String) = {
    val data = map.get(key).get
    val newMap = map + key -> (data + newData)
    newMap
  }
  
  private def getTasks(bout: java.io.ByteArrayOutputStream, interp: scala.tools.nsc.interpreter.IMain, className: java.lang.String) = {
    // list all tasks
    executeLine(interp, "val myTask = " + className.toLowerCase() + ".tasks", bout)
    val mytask = interp.valueOfTerm("myTask")
    mytask match {
      case Some(x) => x.asInstanceOf[List[(String, String)]]
      case None => List[(String, String)]()
    }
  }

  def header = {
    """
      |   _____       _
      |  / ____|     | |
      | | (___   __ _| | _____
      |  \___ \ / _` | |/ / _ \
      |  ____) | (_| |   <  __/
      | |_____/ \__,_|_|\_\___|
    """.stripMargin
  }
}


package com.sake.build

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.Results
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.apache.ivy.ant.IvyResolve
import org.apache.ivy.core.resolve.ResolveEngine
import org.apache.ivy.core.resolve.ResolveEngineSettings
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.sort.SortEngine
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor

object Sake extends App {

  override def main(args: Array[String]) {
    super.main(args)
    val path = if (args.size > 1) args(1) else "."
    Sake.runTargetOnBuild(args(0), path)
  }

  def runTargetOnBuild(target: String, rootPath: String) {
    try {

      val settings = new Settings
      settings.deprecation.value = true // enable detailed deprecation warnings
      settings.unchecked.value = true // enable detailed unchecked warnings
      settings.outputDirs.setSingleOutput("target")

      val pathList = List("bin", "lib/ivy-2.2.0.jar") ::: CompileHelper.compilerPath ::: CompileHelper.libPath
      settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
      settings.classpath.value = (pathList /*::: impliedClassPath*/ ).mkString(File.pathSeparator)

      val buildFile = findBuildFile(rootPath + File.separatorChar + "project").get
      val projectFile = Source.fromFile(buildFile).mkString

      val bout = new ByteArrayOutputStream()
      val interp = new IMain(settings, new PrintWriter(bout))

      val result = interp.interpret(projectFile) // "compile" project file
      result match {
        case Results.Error => println("Error Build compile message: " + String.valueOf(bout))
        case Results.Success => {
          val output = String.valueOf(bout)
          val className = output.substring(output.indexOf("defined class") + ("defined class").size, output.length()).trim()
          println("Build compile message: " + String.valueOf(bout))
          bout.reset()
          val subProjectResult = interp.interpret("new " + className + "().subProjects.map(sp => sp.path).mkString(\",\")")
          val subProjects = String.valueOf(bout)
          if (!subProjects.isEmpty()) {

            val projects = subProjects.split("=")(1).trim()
            if (projects != null && !projects.isEmpty() && !"\"\"".equals(projects)) {
              // compile sub projects
              val projectsPaths = projects.split(",")
              projectsPaths.foreach(projectPath => {
                println("trying to compile project in path " + projectPath)
                println(new File(".").getAbsolutePath())
                runTargetOnBuild(target, projectPath)
                settings.classpath.value = settings.classpath.value + File.pathSeparator + projectPath + File.separatorChar + "target" + File.separatorChar + "classes"
                println("Classpath: " + settings.classpath.value)
              })
              println("This project needs to compile the following projects first: " + projects)
            }
          }
          println("val proj = new " + className + "()")
          println("proj.projectRoot(\"" + rootPath + "\")")
          println("proj.classpath(\"" + settings.classpath.value + "\")")
          println("proj." + target)

          interp.interpret("val proj = new " + className + "()")
          interp.interpret("proj.projectRoot(\"" + rootPath + "\")")
          interp.interpret("proj.classpath(\"" + settings.classpath.value + "\")")
          interp.interpret("proj." + target)

          println("Result: " + result.toString())

        }
        case Results.Incomplete =>
      }

    } catch {
      case e: Throwable => e.printStackTrace()
    }

  }

  def findBuildFile(projectPath: String) = {
    val files = new File(projectPath).listFiles()
    if (files == null) {
      throw new RuntimeException("Unable to find project files in " + projectPath + " relative to " + new File(".").getAbsolutePath())

    }
    println("Files: " + files.mkString(","))
    files.find(f => f.getAbsolutePath().endsWith(".scala"))
  }
}


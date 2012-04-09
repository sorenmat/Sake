package build

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.Results

object InterpeterTest {

  def main(args: Array[String]) {
    InterpeterTest.runTargetOnBuild("compile", "src/TestBuild.scala", ".")
  }

  //  def compile {
  //    val target = "compile" // refac
  //
  //    val settings = new Settings
  //    settings.deprecation.value = true // enable detailed deprecation warnings
  //    settings.unchecked.value = true // enable detailed unchecked warnings
  //    settings.outputDirs.setSingleOutput("target")
  //
  //    val pathList = List("bin") ::: CompileHelper.compilerPath ::: CompileHelper.libPath
  //    settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
  //    settings.classpath.value = (pathList /*::: impliedClassPath*/ ).mkString(File.pathSeparator)
  //
  //    val projectFile = Source.fromFile("src/TestBuild.scala").mkString
  //
  //    val bout = new ByteArrayOutputStream()
  //
  //    val interp = new IMain(settings, new PrintWriter(bout))
  //    val result = interp.interpret(projectFile)
  //    bout.reset()
  //    val subProjectResult = interp.interpret("new TestBuild().subProjects.map(sp => sp.path).mkString(\",\")")
  //    println("subProjectResult: " + String.valueOf(bout))
  //    val subProjects = String.valueOf(bout)
  //    val projects = subProjects.split("=")(1).trim()
  //    if (projects != null && !projects.isEmpty()) {
  //      // compile sub projects
  //      val projectsPaths = projects.split(",")
  //      projectsPaths.foreach(projectPath => {
  //        println(new File(".").getAbsolutePath())
  //        runTargetOnBuild(target, projectPath + "/Build.scala")
  //      })
  //    }
  //    println("This project needs to compile the following projects first: " + projects)
  //
  //    interp.interpret("new TestBuild().compile")
  //    println("Calling packageJar")
  //    interp.interpret("new TestBuild().packageJar")
  //
  //    println("Result: " + result.toString())
  //    //	  result match {
  //    //	    case Success => result.v
  //    //	  }
  //    //    interp.compileString("println(\"test\"")
  //    //    val config = Eval[MyConfig](new File("src/MyConfig.scala"))
  //
  //  }

  def runTargetOnBuild(target: String, buildFile: String, rootPath: String) {
    try {

      val settings = new Settings
      settings.deprecation.value = true // enable detailed deprecation warnings
      settings.unchecked.value = true // enable detailed unchecked warnings
      settings.outputDirs.setSingleOutput("target")

      val pathList = List("bin") ::: CompileHelper.compilerPath ::: CompileHelper.libPath
      settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
      settings.classpath.value = (pathList /*::: impliedClassPath*/ ).mkString(File.pathSeparator)

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
                runTargetOnBuild(target, projectPath + "/SubBuild.scala", projectPath)
                settings.classpath.value = settings.classpath.value + File.pathSeparator + projectPath + File.separatorChar + "target" + File.separatorChar + "classes"
                println("Classpath: "+settings.classpath.value)
              })
            }
            println("This project needs to compile the following projects first: " + projects)
          }
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
}


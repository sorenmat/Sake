package build

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter

import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.Interpreter
import scala.tools.nsc.Settings

object InterpeterTest {

  def main(args: Array[String]) {
    InterpeterTest.compile
  }

  def compile {

    val settings = new Settings
    settings.deprecation.value = true // enable detailed deprecation warnings
    settings.unchecked.value = true // enable detailed unchecked warnings
    settings.outputDirs.setSingleOutput("target")

    val pathList = List("bin") ::: CompileHelper.compilerPath ::: CompileHelper.libPath
    settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
    settings.classpath.value = (pathList /*::: impliedClassPath*/ ).mkString(File.pathSeparator)

    val projectFile = Source.fromFile("src/TestBuild.scala").mkString

    val bout = new ByteArrayOutputStream()

    val interp = new IMain(settings, new PrintWriter(bout))
    val result = interp.interpret(projectFile)
    bout.reset()
    val subProjectResult = interp.interpret("new TestBuild().subProjects.map(sp => sp.path).mkString(\",\")")
    println("subProjectResult: " + String.valueOf(bout))
    interp.interpret("new TestBuild().compile")
    println("Calling packageJar")
    interp.interpret("new TestBuild().packageJar")

    println("Result: " + result.toString())
    //	  result match {
    //	    case Success => result.v
    //	  }
    //    interp.compileString("println(\"test\"")
    //    val config = Eval[MyConfig](new File("src/MyConfig.scala"))

  }

  def runTargetOnBuild(target: String, buildFile: String) {

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

    val result = interp.interpret(projectFile)
    bout.reset()
    val subProjectResult = interp.beSilentDuring("new TestBuild().subProjects")
    println("subProjectResult: " + String.valueOf(bout))
    interp.interpret("new TestBuild()." + target)

    println("Result: " + result.toString())
    //	  result match {
    //	    case Success => result.v
    //	  }
    //    interp.compileString("println(\"test\"")
    //    val config = Eval[MyConfig](new File("src/MyConfig.scala"))

  }

  //    interp.interpret("MyClass.execute");
  //  interpreter.replinfo("\nprintln(\"\\n_______________________________________________________________\\n\")"
  //    )	
}


package com.sake.build

import log.Logger
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import util.FileListing
import com.sake.build.ivy.JarDependency
import com.sun.tools.javac._
import com.sake.build.ivy.IvyResolver
import com.sake.build.ivy.IvyPublisher
import java.io._
import java.util.zip.{ZipEntry, ZipOutputStream}
import java.util.jar.{JarOutputStream, JarEntry}

/**
 * @author soren
 *
 */
trait Build extends Logger {

  lazy val tasks = List(
    ("compile", "compiles the main code"),
    ("test", "running the test code")
  )

  var rootPath = "."
  var internalClasspath = ""

  def sourceFolders: List[String] = List("src")

  def testFolders: List[String] = List("src_test")

  def resourceFolders: List[String] = List("resources")

  def preCompile {}

  def ivyXML = "ivy.xml"

  def javaOptions = List[String]()

  def classpath = {
    IvyResolver.resolveIvyXML(ivyXML).toSet ++ jarDependencies
  }

  def compile = {
    compileFunction(sourceFolders, "classes")
  }

  def compileTest = {
    compileFunction(testFolders, "testclasses", jarTestDependencies.toSeq: _*)
  }

  def compileFunction(folders: List[String], outputDir: String, jarDeps: JarDependency*) = {
    try {

      val settings = new Settings
      settings.deprecation.value = true // enable detailed deprecation warnings
      settings.unchecked.value = true // enable detailed unchecked warnings
      settings.target.value = "1.5"

      settings.outputDirs.setSingleOutput(classOutputDirectory)

      val pathList = CompileHelper.compilerPath ::: CompileHelper.libPath
      settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
      val projectclasspath = classpath.map(f => f.getJarFile.getCanonicalPath()).mkString(File.pathSeparator) + File.pathSeparator + jarDeps.map(f => f.getJarFile.getCanonicalPath()).mkString(File.pathSeparator)
      settings.classpath.value = projectclasspath

      info("\n\n")
      info("**********************************************************************************************************************")
      info("* Compiling " + projectName + " in " + rootPath)
      info("* Classpath " + settings.classpath.value)
      info("* Compiling files in " + folders.mkString(","))
      info("* Build root path = " + rootPath)
      info("**********************************************************************************************************************")

      val scalaFilesToCompile = folders.flatMap(folder => FileListing.getFileListing(new File(rootPath, folder), f => {
        f.getAbsoluteFile().toString().endsWith(".scala")
      }).map(f => f.getCanonicalPath()))

      val javaFilesToCompile = folders.flatMap(folder => FileListing.getFileListing(new File(rootPath, folder), f => {
        f.getAbsoluteFile().toString().endsWith(".java")
      }).map(f => f.getCanonicalPath()))

      if (!javaFilesToCompile.isEmpty) {

        // java compile
        info("Compiling java")
        val writer = new PrintWriter("/tmp/test.txt")
        val sourcePath = folders.map(sf => new File(rootPath, sf).getCanonicalPath()).mkString("" + File.pathSeparatorChar)
        info("SourcePath: " + sourcePath)
        val javaParameters = (javaOptions ::: List("-cp", settings.classpath.value, "-d", rootPath + File.separator + "target/" + outputDir, "-sourcepath", sourcePath) ::: javaFilesToCompile).toArray
        //        println("Java parameters: "+javaParameters.mkString(", "))
        val result = exec(javaParameters, writer)
        if (result != 0)
          throw new RuntimeException("Error compiling java code")
        info("Done compiling java with result " + result)
      }

      // scala compile
      if (!scalaFilesToCompile.isEmpty) {
        info("Trying to compile the following files: " + scalaFilesToCompile.mkString("\n"))
        info("Compiling scala")
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
    info("Creating Jar '" + jarName + "' in directory " + new File(jarOutputDirectory).getCanonicalPath)
    assembleJar(toInclude(classOutputDirectory), jarName)
  }

  /**
   * Creates a jar file containg all the test classes
   *
   * @return jarfile
   */
  def packageTestJar = {
    info("Creating Jar '" + jarName + "' in directory " + new File(jarOutputDirectory).getCanonicalPath)
    assembleJar(toInclude(testClassOutputDirectory), testJarName)
  }

  def publish {
    info("publish local..." + projectName)
    debug("Publish depenency " + classpath.map(f => f.toString()).mkString(", "))
    IvyPublisher.publish(new File(jarName), "com.schantz", projectName, "1.0", classpath)
  }

  def jarName = {
    jarOutputDirectory + File.separatorChar + projectName + ".jar"
  }
  def testJarName = {
    jarOutputDirectory + File.separatorChar + projectName + "Test.jar"
  }

  def jarOutputDirectory = {
    val dir = rootPath + File.separatorChar + outputDirectory + File.separatorChar + "jars"
    new File(dir).mkdirs()
    dir
  }

  def projectName: String

  def mainClass = ""

  def classOutputDirectory = {
    //FIXME split up test and main
    val dir = rootPath + File.separatorChar + outputDirectory + File.separatorChar + "classes"
    new File(dir).mkdirs()
    dir
  }

  def testClassOutputDirectory = {
    val testdir = rootPath + File.separatorChar + outputDirectory + File.separatorChar + "testclasses"
    new File(testdir).mkdirs()
    testdir
  }

  def outputDirectory = "target"

  /**
   * Defines list of sub projects the this build depends on
   */
  def subProjects: List[SubProject] = Nil

  def jarDependencies: Set[JarDependency] = Set()

  def jarTestDependencies: Set[JarDependency] = Set(new JarDependency(jarName))


  /**
   * List of tuples to include in the jar file, this includes both class files and resources
   *
   * @param classDirectory
   * @return
   */
  def toInclude(classDirectory: String) : List[(String, String)] = {
    val resourceFiles = resourceFolders.flatMap(folderPath => recursiveListFiles(new File(folderPath)).map(f => (f.getCanonicalPath, f.getCanonicalPath.replace(new File(folderPath).getCanonicalPath+File.separator, ""))).toList)

    val classFiles = recursiveListFiles(new File(classDirectory)).map(f => {
      (f.getAbsolutePath, f.getCanonicalPath.replace(new File(classDirectory).getCanonicalPath+File.separator, ""))

    }).toList
    val newfiles = classFiles ::: resourceFiles
    debug("Files: " + newfiles.mkString("\n\t"))
    newfiles
  }


  /**
   * Method to assemble the actual jar file
   *
   * @param fileNames
   * @param outputFile
   */
  def assembleJar(fileNames: List[(String, String)], outputFile: String) {
    val manifest = new java.util.jar.Manifest()
    //manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0")
    //manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MAIN_CLASS, "TestClass")
    val target = new JarOutputStream(new FileOutputStream(outputFile), manifest)
    fileNames.foreach(entry => {
      val (filePath, jarPath) = entry
      add(new File(filePath), jarPath, target)
    })
    target.close()
  }

  def add(source: File, jarPath: String, target: JarOutputStream) {

    try {
      /*
      if (source.isDirectory()) {
        var name = source.getPath().replace("\\", "/");
        if (!name.isEmpty()) {
          if (!name.endsWith("/"))
            name = name + "/";
          val entry = new JarEntry(name);
          entry.setTime(source.lastModified());
          target.putNextEntry(entry);
          target.closeEntry();
        }
        for (nestedFile <- source.listFiles())
          add(nestedFile, target);
      } */
      val entry = new JarEntry(jarPath);
      entry.setTime(source.lastModified());
      target.putNextEntry(entry);
      val in = new BufferedInputStream(new FileInputStream(source));

      val buffer = new Array[Byte](1024)
      var count = 0
      while (count != -1) {
        count = in.read(buffer);
        if (count != -1)
          target.write(buffer, 0, count);
      }
      target.closeEntry();
      in.close();
    }
  }


  /**
   *  Filter used in finding files to include in the jar file. One could override this method in order to exclude files in the jar.
   *  This is called both from packageJar and packageTestJar
   * @param file
   * @return
   */
  def defaultFilter(file: File): Boolean = {
    if (file.getName.startsWith("."))
      false
    else
      true
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles.filter(file => !file.isDirectory && defaultFilter(file))
    these ++ f.listFiles.filter(file => file.isDirectory && defaultFilter(file)).flatMap(recursiveListFiles)
  }

}

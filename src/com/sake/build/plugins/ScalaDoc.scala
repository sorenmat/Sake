package com.sake.build.plugins

import com.sake.build.Build
import java.io.File

trait ScalaDoc extends Build {


  def doc = scaladoc

  def scaladoc = {
    val projectclasspath = classpath.map(f => f.getJarFile.getCanonicalPath()).mkString(File.pathSeparator)
    val resourceFiles = sourceFolders.flatMap(folderPath => recursiveListFiles(new File(folderPath)).map(f => (f.getCanonicalPath)).toList)

    new File("target/docs").mkdirs()

    val args = Array("-classpath", projectclasspath,"-d","target/docs", "-sourcepath", sourceFolders.mkString(File.pathSeparator) )
    val sd = scala.tools.nsc.ScalaDoc.main(args)

  }
}

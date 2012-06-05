package com.sake.build.plugins

import java.io.File
import scala.xml._
import com.sake.build.ivy._
import com.sake.build.{SubProject, Build}

trait EclipseBuild extends Build {

  override def subProjects = {
    val classpathFile = new File(rootPath, ".classpath")
    println("classpath file " + classpathFile)
    val entries = XML.loadFile(classpathFile) \\ "classpathentry"
    entries.filter(f => (((f \\ "@kind").text == "src") && (f \\ "@combineaccessrules").text == "false")).map(entry => {

      val projectName = (entry \\ "@path").text.replace(File.separator, "")
      println("Trying to find " + projectName)
      val f = find(projectName, new File(rootPath)).get
      println("Project depens = " + f)
      new SubProject(f)
    }).toList
  }

  override def jarDependencies = {
    val jarRepository = find("JarRepository", new File(rootPath)).head.getParentFile.getAbsolutePath

    val classpathFile = new File(rootPath, ".classpath")
    val entries = XML.loadFile(classpathFile) \\ "classpathentry"
    entries.filter(f => f \\ "@kind" != "lib").map(entry => {

      val path = (entry \\ "@path").text;
      val f = if (path.matches("/.*")) {
        new File(jarRepository, path).getCanonicalPath
      } else {

        new File((entry \\ "@path").toString).getCanonicalPath
      }

      new JarDependency(f)
    }).toSet ++ super.jarDependencies
  }

  /*
    *   finds a directory with a specified name..
    */
  def find(name: String, currentDir: File): Option[File] = {
    var parentPath = new File(getParentDirectory(currentDir.getAbsolutePath))
    var notFound = true
    while (parentPath.getAbsolutePath != "/") {
      println("Searching: " + parentPath.getAbsolutePath)
      val found = searchDown(parentPath, name)
      found match {
        case Some(x) => return Some(x)
        case None => parentPath = new File(getParentDirectory(parentPath.getAbsolutePath))
      }
    }
    None
  }

  def searchDown(aStartingDir: File, name: String): Option[File] = {
    var result = None
    val filesAndDirs = aStartingDir.listFiles()
    for (file <- filesAndDirs) {
      if (file.getName == ".project") {
        val projectName = (XML.loadFile(file) \\ "projectDescription" \ "name").text
        if (projectName == name)
          return Some(new File(getParentDirectory(file.getAbsolutePath)))
        else
          return None
      }

      if (file.isDirectory()) {
        val deeperList = searchDown(file, name)
        deeperList match {
          case Some(x) => return Some(x)
          case None =>
        }
      }
    }
    return result;
  }

  def getParentDirectory(dir: String) = {
    val tmp = if (dir.endsWith("" + File.separatorChar)) dir.substring(0, dir.size - 1) else dir
    tmp.substring(0, tmp.lastIndexOf(File.separatorChar))
  }
}
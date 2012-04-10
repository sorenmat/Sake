package com.sake.build.ivy
import java.io.File

class JarDependency(val groupId: String, val artifactId: String, val version: String) {

  var resolve = true
  var jarFile = ""

  def this(jarFile: String) = {
    this("", "", "")
    resolve = false
    this.jarFile = jarFile
  }

  def getJarFile = {
    if (resolve)
      IvyResolver.resolve(this)
    else
      new File(jarFile)
  }
}
object testing {
  new JarDependency("")
}
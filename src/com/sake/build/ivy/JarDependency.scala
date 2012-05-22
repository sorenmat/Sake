package com.sake.build.ivy
import java.io.File

class JarDependency(val organization: String, val name: String, val revision: String) {

  var resolve = true
  var jarFile = ""

  def this(jarFile: String) = {
    this("", "", "")
    resolve = false
    this.jarFile = jarFile
  }

  def getJarFile = {
    if (resolve)
      IvyResolver.resolve(this).get
    else
      new File(jarFile)
  }
}
object testing {
  new JarDependency("")
}
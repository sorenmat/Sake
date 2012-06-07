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
      new IvyResolver().resolve(this).get
    else
      new File(jarFile)
  }

  override def toString = organization+":"+name+":"+revision
}
object testing {
  new JarDependency("")
}
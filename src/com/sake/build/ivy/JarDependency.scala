package com.sake.build.ivy
import java.io.File

case class JarDependency(groupId: String, artifactId: String, version: String) {
  
  var resolve = true
  var jarFile = ""
    
  def this(jarFile: String) {
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
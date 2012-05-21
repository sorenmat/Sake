package com.sake.build
import java.io.File

class SubProject(fullPath: File) {

  def this(relativePath: String) = {
    this(new File(relativePath).getCanonicalFile())
  }
  
  def path: String = fullPath.getCanonicalPath()
}
package util

import java.io._

object FileListing {

  /**
   * Recursively walk a directory tree and return a List of all
   * Files found; the List is sorted using File.compareTo().
   *
   * @param aStartingDir is a valid directory, which can be read.
   */
  def getFileListing(aStartingDir: File) : List[File] = {
    validateDirectory(aStartingDir);
    val result = getFileListing(aStartingDir, f => true);
    //    Collections.sort(result);
    result;
  }

  // PRIVATE //
  def getFileListing(aStartingDir: File, filter: (File) => Boolean): List[File] = {
    var result: List[File] = List()
    val filesAndDirs = aStartingDir.listFiles();
    if (filesAndDirs != null) {

      for (file <- filesAndDirs) {
        if (filter(file))
          result = file :: result
        if (!file.isFile()) {
          //must be a directory
          //recursive call!
          val deeperList = getFileListing(file, filter)
          result = result ::: deeperList
        }
      }
    }
    result;
  }

  /**
   * Directory is valid if it exists, does not represent a file, and can be read.
   */
  def validateDirectory(
    aDirectory: File) = {
    if (aDirectory == null) {
      throw new IllegalArgumentException("Directory should not be null.");
    }
    if (!aDirectory.exists()) {
      throw new FileNotFoundException("Directory does not exist: " + aDirectory);
    }
    if (!aDirectory.isDirectory()) {
      throw new IllegalArgumentException("Is not a directory: " + aDirectory);
    }
    if (!aDirectory.canRead()) {
      throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
    }
  }

  def main(args: Array[String]) {
    println(FileListing.getFileListing(new File("/tmp"), f => f.getAbsoluteFile().toString().endsWith(".scala")).mkString(", "))
  }
} 

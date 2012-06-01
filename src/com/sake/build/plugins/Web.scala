package com.sake.build.plugins

import tools.nsc.io.{Jar, ZipArchive}
import java.util.zip.{ZipEntry, ZipOutputStream}
import java.io.{IOException, FileOutputStream, FileInputStream, File}
import com.sake.build.Build
import org.specs2.files

/**
 * Trait for adding support for building war files
 */
trait Web extends Build {

  /**
   * Root directory to look for web resources (.js, .css, .html etc.) to be included in the war file
   */
  def webRoot: String

  /**
   * The directory where the war file will be created
   */
  def warTargetDirectory = "target/war"

  /**
   * The filename of the war file, default is project name with the war extension
   */
  def warFileName = projectName + ".war"

  /**
   * Sake task to create a war file
   */
  def packageWar {
    val webDir = new File(webRoot)
    if (!webDir.exists())
      throw new RuntimeException("Unable to find webroot " + webRoot)

    // create the output directory
    new File(warTargetDirectory).mkdirs
    val output = warTargetDirectory+File.separator+warFileName
    createWarFile(output, includedFiles(webDir))
  }


  /**
   * Method to find the files to be included in the war package.
   * This should be overridden if there are changes to what should be included in the war
   *
   * @param webDir The web root source directory to include files from
   * @return a list of tuples (fullPath to File, path inside war file)
   */
  def includedFiles(webDir: File) = {
    val files = recursiveListFiles(webDir).map(f => (f.getCanonicalPath, f.getCanonicalPath.replace(webRoot, ""))).toList
    val jarFiles = classpath.map(jar => {
      val jarPath = jar.getJarFile.getAbsolutePath
      val zipPath = "WEB-INF/lib/"+jar.getJarFile.getName
      (jarPath, zipPath)
    })
    if(!new File(outputDirectory).exists())
      throw new RuntimeException("uable to find output directory "+outputDirectory)

    val classFiles = recursiveListFiles(new File(classOutputDirectory)).map(f => {
      (f.getAbsolutePath, f.getCanonicalPath.replace(new File(classOutputDirectory).getCanonicalPath, "WEB-INF/classes"))

    }).toList
    val newfiles = files ::: jarFiles.toList ::: classFiles
    println("Files: "+newfiles.mkString("\n\t"))
    newfiles
  }

  /**
   * creates the jar file from the files specified via the includedFiles method
   *
   * @param outFilename Name of the war file
   * @param filenames List of tuples (fullPath to File, path inside war file)
   */
  def createWarFile(outFilename: String,  filenames: List[(String, String)]) {
    val buf = new Array[Byte](1024)

    try {
      // Create the ZIP file
      val out = new ZipOutputStream(new FileOutputStream(outFilename))

      // Compress the files
      filenames.foreach(filename => {
        val (fullPath, zipPath) = filename
        val in = new FileInputStream(fullPath)

        // Add ZIP entry to output stream.
        //println("Adding to zip "+zipPath)
        out.putNextEntry(new ZipEntry(zipPath))

        Stream.continually(in.read(buf)).takeWhile(_ != -1).foreach(
           out.write(buf, 0, _)
        )

        // Complete the entry
        out.closeEntry()
        in.close()
      })

      // Complete the ZIP file
      out.close()
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles.filter(!_.isDirectory)
    these ++ f.listFiles.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
}


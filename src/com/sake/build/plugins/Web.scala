package com.sake.build.plugins

import tools.nsc.io.{Jar, ZipArchive}
import java.util.zip.{ZipEntry, ZipOutputStream}
import java.io.{IOException, FileOutputStream, FileInputStream, File}
import com.sake.build.Build

/**
 * Created by IntelliJ IDEA.
 * User: soren
 * Date: 5/30/12
 * Time: 16:58
 * To change this template use File | Settings | File Templates.
 */

trait Web extends Build {

  def webRoot: String

  def packageWar = {
    val webDir = new File(webRoot)
    if (!webDir.exists())
      throw new RuntimeException("Unable to find webroot " + webRoot)


    createWarFile("/tmp/test.zip", includedFiles(webDir))
  }

  def includedFiles(webDir: File) = {
    val files = recursiveListFiles(webDir).map(f => (f.getAbsolutePath, f.getAbsolutePath.replace(webRoot, ""))).toList
    val jarFiles = jarDependencies.map(jar => {
      val jarPath = jar.getJarFile.getAbsolutePath
      val zipPath = "WEB-INF/lib/"+jar.getJarFile.getName
      (jarPath, zipPath)
    })
    if(!new File(outputDirectory).exists())
      throw new RuntimeException("uable to find output directory "+outputDirectory)
    val classFiles = recursiveListFiles(new File(outputDirectory)).map(f => (f.getAbsolutePath, f.getAbsolutePath.replace(outputDirectory, "WEB-INF/classes"))).toList
    val newfiles = files ::: jarFiles ::: classFiles
    println("Files: "+newfiles.mkString("\n\t"))
    newfiles
  }

  def createWarFile(outFilename: String,  filenames: List[(String, String)]) {


    val buf = new Array[Byte](1024)

    try {
      // Create the ZIP file
      val out = new ZipOutputStream(new FileOutputStream(outFilename));

      // Compress the files
      filenames.foreach(filename => {
        val (fullPath, zipPath) = filename
        val in = new FileInputStream(fullPath);

        // Add ZIP entry to output stream.
        println("Adding to zip "+zipPath)
        out.putNextEntry(new ZipEntry(zipPath));

        // Transfer bytes from the file to the ZIP file

        Stream.continually(in.read(buf)).takeWhile(_ != -1).foreach(
          //md.update(buffer, 0, _)
           out.write(buf, 0, _)
        )

        // Complete the entry
        out.closeEntry();
        in.close();
      })

      // Complete the ZIP file
      out.close();
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles.filter(!_.isDirectory)
    these ++ f.listFiles.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
}


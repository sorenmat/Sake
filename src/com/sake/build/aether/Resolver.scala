package com.sake.build.aether


import io.Source
import java.net.URL
import java.security.MessageDigest
import java.io._
import java.nio.channels.FileChannel.MapMode
import java.nio.channels.FileChannel
import com.sake.build.log.Logger
import xml.XML


object Resolver extends Logger {
  val home = System.getProperty("user.home")

  def resolver(org: String, module: String, revision: String, artifact: String, transitive: Boolean): File = {
    val baseURL = "http://repo1.maven.org/maven2/" + "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"
    val resolvedURL = baseURL.replace("[organisation]", org.replace(".", "/")).replace("[module]", module).replace("[revision]", revision).replace("[artifact]", artifact).replace("[ext]", "jar")
    if (transitive) {
      val pomResolvedURL = baseURL.replace("[organisation]", org.replace(".", "/")).replace("[module]", module).replace("[revision]", revision).replace("[artifact]", artifact).replace("[ext]", "pom")
      info("Resolved URL " + pomResolvedURL)

      val pomXML = XML.load(Source.fromURL(pomResolvedURL).reader())
      val deps = pomXML \\ "dependency"
      deps.foreach(dep => {
        val org = (dep \\ "groupId").text
        val module = (dep \\ "artifactId").text
        val revision = (dep \\ "version").text
        if (revision != "")
          resolver(org, module, revision, module, true)
        else
          info("missing revision for "+org+":"+module+":"+artifact)
      })

    }

    info("Resolved URL " + resolvedURL)
    val reader = new URL(resolvedURL).openConnection().getInputStream

    val buffer = new Array[Byte](2048)

    val localFile = localFilename(org, module, revision, artifact)
    val fetchFile = {
      if (new File(localFile).exists()) {
        val localFileMD5 = md5Checksum(localFile)
        val remoteMD5 = getRemoteMD5Checksum(org, module, revision, artifact)
        localFileMD5 != remoteMD5
      } else true

    }
    if (fetchFile) {
      info("Artifact not found in local cache, downloading it")
      try {
        val created = new File(localPath(org, module, revision)).mkdirs()
      } catch {
        case e: Exception => e.printStackTrace()
      }
      val out = new BufferedOutputStream(new FileOutputStream(localFile))

      info_nolinebreak("Downloading "+org+":"+module+":"+artifact+" -> ")
      Iterator.continually(reader.read(buffer)).takeWhile(b => {
        print("#")
        b != -1
      }).foreach(n => out.write(buffer, 0, n))
      println
      out.close()
    } else {
      // return local cached version
      info("using cached version of " + org + ":" + module + ":" + revision)
    }
    return new File(localFilename(org, module, revision, artifact))
  }

  def getRemoteMD5Checksum(org: String, module: String, revision: String, artifact: String) = {
    //http://repo1.maven.org/maven2/org/apache/maven/maven-profile/2.2.1/maven-profile-2.2.1.jar.asc.md5
    val baseURL = "http://repo1.maven.org/maven2/" + "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"
    val resolvedURL = baseURL.replace("[organisation]", org.replace(".", "/")).replace("[module]", module).replace("[revision]", revision).replace("[artifact]", artifact).replace("[ext]", "jar.md5")
    Source.fromURL(resolvedURL).mkString
  }


  def localPath(org: String, module: String, revision: String) = {
    val baseURL = home + "/.ivy2/cache/" + "[organisation]/[module]/[revision]"
    baseURL.replace("[organisation]", org).replace("[module]", module).replace("[revision]", revision)
  }

  def localFilename(org: String, module: String, revision: String, artifact: String) = {
    val baseURL = home + "/.ivy2/cache/" + "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"
    baseURL.replace("[organisation]", org).replace("[module]", module).replace("[revision]", revision).replace("[artifact]", artifact).replace("[ext]", "jar")
  }

  def md5Checksum(file: String) = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(fileToBytes(file))

    md5.digest().map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
  }

  def fileToBytes(fileName: String): Array[Byte] = {
    val f = new File(fileName);
    var fin: FileInputStream = null;
    var ch: FileChannel = null;
    try {
      fin = new FileInputStream(f);
      ch = fin.getChannel();
      val size = ch.size();
      val buf = ch.map(MapMode.READ_ONLY, 0, size);
      val bytes = new Array[Byte](size.toInt)
      buf.get(bytes);
      return bytes
    } catch {
      case e: IOException => e.printStackTrace(); return Array[Byte]()
    } finally {
      try {
        if (fin != null) {
          fin.close();
        }
        if (ch != null) {
          ch.close();
        }
      } catch {
        case e: IOException => e.printStackTrace();
      }
    }
  }

  def main(args: Array[String]) {
    resolver("org.slf4j", "slf4j-jcl", "1.6.5", "slf4j-jcl", true)
  }
}

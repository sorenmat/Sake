package com.sake.build.ivy
import scala.collection.JavaConversions._
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.Ivy
import org.apache.ivy.plugins.resolver.FileSystemResolver
import java.io.File
import org.apache.ivy.core.LogOptions
import scala.io.Source._
import com.sake.build.util.RichFile
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq

object IvyPublisher {

  private def getIvy = {
    val ivySettings = new IvySettings()
    //url resolver for configuration of maven repo
    val resolver = new FileSystemResolver()
    resolver.setM2compatible(true)
    resolver.setName("local")
    //you can specify the url resolution pattern strategy
    //    resolver.addArtifactPattern("http://repo1.maven.org/maven2/" + "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]")
    //adding maven repo resolver
    ivySettings.addResolver(resolver)
    //set to the default resolver
    ivySettings.setDefaultResolver(resolver.getName())
    //creates an Ivy instance with settings
    val ivy = Ivy.newInstance(ivySettings)
    ivy
  }

  private def defaultresolveOptions = {
    val confs = List("default").toArray
    val resolveOptions = new ResolveOptions().setConfs(confs)
    resolveOptions.setOutputReport(false)
    resolveOptions.setLog(LogOptions.LOG_DOWNLOAD_ONLY)
    resolveOptions
  }

  def publish(org: String, name: String, revision: String, deps: List[JarDependency]) = {
    try {

      val cacheDir = new File("/Users/soren/.ivy2/cache")
      if (!new File(cacheDir, org + File.separator + name + File.separator + "jars").exists()) {

        val file = new File(cacheDir, org + File.separator + name + File.separator + "jars")
        println(file.getCanonicalPath())
        file.mkdirs()
      }

      val toFile = new File(new File(cacheDir, org + File.separator + name) + File.separator + "jars", "foundation-1.0.jar")
      val fromFile = new File("/Users/soren/code/buildtest/Foundation/target/jars/Foundation.jar")
      copyFile(fromFile, toFile)

      val ivyFile = new File(cacheDir, org + File.separator + name + File.separator + "ivy-" + revision + ".xml")
      println("Creating ivy file " + ivyFile.getCanonicalPath())
      createIvyFile(ivyFile, org, name, revision, deps)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def createIvyFile(file: File, org: String, module: String, rev: String, deps: List[JarDependency]) {
    val f = new RichFile(file)
    f.text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ivyPublishFile(org, module, rev, "release", "20121215185411", deps).toString()

  }

  def ivyPublishFile(org: String, module: String, rev: String, status: String, pubDate: String, deps: List[JarDependency]) = {
    println("deps: " + deps.size)
    <ivy-module version="2.0" xmlns:m="http://ant.apache.org/ivy/maven">
      <info organisation={ org } module={ module } revision={ rev } status={ status } publication={ pubDate }>
      </info>
      <publications>
        <artifact name="foundation" type="jar" ext="jar" conf="master"/>
      </publications>
      {
        if (!deps.isEmpty) {
          val xml = <dependencies> { deps.map(elm => <dependency org={ elm.organization } name={ elm.name } rev={ elm.revision } force="true"/>) } </dependencies>
          xml
        } else
          NodeSeq.Empty
      }
    </ivy-module>
  }
  def copyFile(from: File, to: File): Boolean = {
    import java.io.{ FileInputStream, FileOutputStream }
    import java.nio.channels.FileChannel
    val inChannel: FileChannel = new FileInputStream(from).getChannel
    val outChannel: FileChannel = new FileOutputStream(to).getChannel
    try {
      // magic number for Windows, 64Mb - 32Kb)
      val maxCount: Int = (64 * 1024 * 1024) - (32 * 1024)
      val size: Long = inChannel.size
      var position: Long = 0
      while (position < size) {
        position += inChannel.transferTo(position, maxCount, outChannel)
      }
      true
    } catch {
      case _ => false
    } finally {
      if (inChannel != null) inChannel.close
      if (outChannel != null) outChannel.close
    }
  }

  def main(args: Array[String]) {
    val deps = List(new JarDependency("commons-collections", "commons-collections", "3.1"))
    publish("com.schantz", "foundation", "1.0", deps)
    val file = IvyResolver.resolve(new JarDependency("com.schantz", "foundation", "1.0"))
    println(file.get.getCanonicalPath())
  }
}

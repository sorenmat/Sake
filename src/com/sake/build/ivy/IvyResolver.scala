package com.sake.build.ivy

import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.Ivy
import java.io.File
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.LogOptions
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import com.sake.build.log.Logger
import scala.collection.JavaConversions._
import org.apache.ivy.plugins.resolver.{URLResolver, FileSystemResolver}
import xml.XML
import com.sake.build.aether.Resolver
import sun.tools.jar.resources.jar

/**
 * Resolves dependencies via ivy.
 * @author soren
 *
 */
class IvyResolver(offline: Boolean = false) extends Logger {

  def resolve(jarDep: JarDependency) = {
    //creates clear ivy settings
    val file = new Resolver(offline).resolver(jarDep)
    if (file != null)
      Some(file)
    else
      None
  }

  def resolveIvyXML(file: String) : Seq[JarDependency] = {
    val xml = XML.load(file)
    val deps = xml \\ "dependency"
    val result = deps.map(dep => {
      val org = (dep \\ "@org").text
      val name = (dep \\ "@name").text
      val revision = (dep \\ "@rev").text
      val transitive = (dep \\ "@transitive").text == "true"
      new JarDependency(org, name, revision)
    })
    println("Result: "+result.mkString("\n"))
    result
  }

  def main(args: Array[String]) {
    resolveIvyXML("/Users/soren/workspaces/UnitLinked/GeneralAdvLife/ivy.xml")    .foreach(dep => dep.getJarFile)
    //resolve(new JarDependency("commons-lang", "commons-lang", "1.0"))
  }

}
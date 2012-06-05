package com.sake.build.ivy
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.Ivy
import java.io.File
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.LogOptions
import org.apache.ivy.util.MessageLogger
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import scala.collection.JavaConversions._
import com.sake.build.log.Logger

/**
 * Resolves dependencies via ivy.
 * @author soren
 *
 */
object IvyResolver extends Logger {

  lazy val getIvy = {
    val ivyFile = new IvySettings()
    val settingsFile = new java.io.File("ivysettings.xml")
    if (settingsFile.exists())
      ivyFile.load(settingsFile)
    else
      debug("Unable to find ivysettings.xml")

    //    creates an Ivy instance with settings
    val ivy = Ivy.newInstance(ivyFile)
    ivy.getLoggerEngine.setDefaultLogger(new DefaultMessageLogger(Message.MSG_ERR))
    ivy
  }

  private def defaultresolveOptions = {
    val confs = List("default").toArray
    val resolveOptions = new ResolveOptions().setConfs(confs)
    resolveOptions.setOutputReport(false)
    resolveOptions.setLog(LogOptions.LOG_DOWNLOAD_ONLY)
    resolveOptions
  }

  def resolve(jarDep: JarDependency) = {
    //creates clear ivy settings
    val ivy = getIvy
    val ivyfile = File.createTempFile("ivy", ".xml")
    ivyfile.deleteOnExit()

    val dep = List(jarDep.organization, jarDep.name, jarDep.revision).toArray

    val md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep(0), dep(1) + "-caller", "working"))

    val dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(dep(0), dep(1), dep(2)), false, false, true)
    md.addDependency(dd)

    //creates an ivy configuration file
    XmlModuleDescriptorWriter.write(md, ivyfile)

    //init resolve report
    val report = ivy.resolve(ivyfile.toURI().toURL(), defaultresolveOptions)

    //so you can get the jar library
    val artifacts = report.getAllArtifactsReports()
    val result = if (artifacts.size > 0) {
      val arts = artifacts.map(f =>
        if (f.getArtifact.getConfigurations.contains("master") || f.getArtifact.getConfigurations.contains("default"))
          Some(f.getLocalFile)
        else
          None)
      val tmp = arts.flatMap(f => f)
      Some(tmp(0))
    } else
      None
    result
  }

  def resolveIvyXML(file: String) = {
    try {
      val ivyfile = new File(file)
      if (ivyfile.exists) {

        //init resolve report
        val report = getIvy.resolve(ivyfile.toURI().toURL(), defaultresolveOptions)
        if (report.hasError) {

          val problems = report.getAllProblemMessages()
          if (problems != null && !problems.isEmpty()) {
            val errorMsgs = new StringBuffer()
            for (problem <- problems) {
              errorMsgs.append(problem)
              errorMsgs.append("\n")
            }
            error("Errors encountered during dependency resolution for package :")
            error(errorMsgs.toString)
            throw new RuntimeException("unable to resolve dependcies.")

          }
        } else {
          info("Dependencies in file " + file + " were successfully resolved")
        }
        //so you can get the jar library
        val jarArtifactFile = report.getAllArtifactsReports().map(f => {
          val moduleRevId = f.getArtifact().getModuleRevisionId()
          new JarDependency(moduleRevId.getOrganisation(), moduleRevId.getName(), moduleRevId.getRevision())
        })
        jarArtifactFile
      } else {
        debug("Unable to find ivy.xml")
        Array[JarDependency]()
      }
    } catch {
      case t: Throwable => throw new RuntimeException(t)
    }
  }

  def main(args: Array[String]) {
    resolve(new JarDependency("commons-lang", "commons-lang" ,"1.0"))
  }

}
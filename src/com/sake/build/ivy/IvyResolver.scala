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

/**
 * Resolves dependencies via ivy.
 * @author soren
 *
 */
object IvyResolver {

  lazy val getIvy = {
    //    val ivySettings = new IvySettings()
    //    //url resolver for configuration of maven repo
    //    val resolver = new URLResolver()
    //    resolver.setM2compatible(true)
    //    resolver.setName("central")
    //    //you can specify the url resolution pattern strategy
    //    resolver.addArtifactPattern("http://repo1.maven.org/maven2/" + "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]")
    //    //adding maven repo resolver
    //    ivySettings.addResolver(resolver)
    //    //set to the default resolver
    //    ivySettings.setDefaultResolver(resolver.getName())
    val ivyFile = new IvySettings()
    ivyFile.load(new java.io.File("/Users/soren/code/buildtest/Foundation/ivysettings.xml"))
    //    creates an Ivy instance with settings
    val ivy = Ivy.newInstance(ivyFile)
    ivy.getLoggerEngine.setDefaultLogger(new DefaultMessageLogger(Message.MSG_INFO))
    ivy
  }

  private def defaultresolveOptions = {
    val confs = List("default").toArray
    val resolveOptions = new ResolveOptions().setConfs(confs)
    resolveOptions.setOutputReport(false)
    resolveOptions.setLog(LogOptions.LOG_DOWNLOAD_ONLY)
    //    resolveOptions.setTransitive(false)
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
      println(artifacts(0).getArtifact.getConfigurations.mkString(", "));
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

          val problems = report.getAllProblemMessages();
          if (problems != null && !problems.isEmpty()) {
            val errorMsgs = new StringBuffer();
            for (problem <- problems) {
              errorMsgs.append(problem);
              errorMsgs.append("\n");
            }
            System.err.println("Errors encountered during dependency resolution for package :");
            System.err.println(errorMsgs);
          }
        } else {
          System.out.println("Dependencies in file " + file + " were successfully resolved");
        }
        //so you can get the jar library
        val jarArtifactFile = report.getAllArtifactsReports().map(f => {
          val moduleRevId = f.getArtifact().getModuleRevisionId()
          new JarDependency(moduleRevId.getOrganisation(), moduleRevId.getName(), moduleRevId.getRevision())
        })
        jarArtifactFile
      } else {
        println("Unable to find ivy.xml")
        Array[JarDependency]()
      }
    } catch {
      case t: Throwable => throw new RuntimeException(t)
    }
  }

}
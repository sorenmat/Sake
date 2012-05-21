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

/**
 * Resolves dependencies via ivy. 
 * @author soren
 *
 */
object IvyResolver {
  
  private def getIvy = {
    val ivySettings = new IvySettings()
    //url resolver for configuration of maven repo
    val resolver = new URLResolver()
    resolver.setM2compatible(true)
    resolver.setName("central")
    //you can specify the url resolution pattern strategy
    resolver.addArtifactPattern("http://repo1.maven.org/maven2/" + "[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]")
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
  
  def resolve(jarDep: JarDependency) = {
    //creates clear ivy settings
    val ivy = getIvy
    val ivyfile = File.createTempFile("ivy", ".xml")
    ivyfile.deleteOnExit()

    val dep = List(jarDep.groupId, jarDep.artifactId, jarDep.version).toArray

    val md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep(0), dep(1) + "-caller", "working"))

    val dd = new DefaultDependencyDescriptor(md,
      ModuleRevisionId.newInstance(dep(0), dep(1), dep(2)), false, false, true)
    md.addDependency(dd)

    //creates an ivy configuration file
    XmlModuleDescriptorWriter.write(md, ivyfile)



    //init resolve report
    val report = ivy.resolve(ivyfile.toURI().toURL(), defaultresolveOptions)

    //so you can get the jar library
    val jarArtifactFile = report.getAllArtifactsReports()(0).getLocalFile()
    jarArtifactFile

  }
  
  def resolveIvyXML(file: String) = {
    val ivyfile = new File(file) 
    //init resolve report
    val report = getIvy.resolve(ivyfile.toURI().toURL(), defaultresolveOptions)

    //so you can get the jar library
    val jarArtifactFile = report.getAllArtifactsReports().map(f => f.getLocalFile())
    jarArtifactFile
  }
}
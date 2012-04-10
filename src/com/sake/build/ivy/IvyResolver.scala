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

object IvyResolver {
  def resolve(jarDep: JarDependency) = {
    //creates clear ivy settings
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

    val ivyfile = File.createTempFile("ivy", ".xml")
    ivyfile.deleteOnExit()

    val dep = List(jarDep.groupId, jarDep.artifactId, jarDep.version).toArray

    val md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep(0), dep(1) + "-caller", "working"))

    val dd = new DefaultDependencyDescriptor(md,
      ModuleRevisionId.newInstance(dep(0), dep(1), dep(2)), false, false, true)
    md.addDependency(dd)

    //creates an ivy configuration file
    XmlModuleDescriptorWriter.write(md, ivyfile)

    val confs = List("default").toArray
    val resolveOptions = new ResolveOptions().setConfs(confs)

    //init resolve report
    val report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions)

    //so you can get the jar library
    val jarArtifactFile = report.getAllArtifactsReports()(0).getLocalFile()
    println(jarArtifactFile.getAbsolutePath())
    jarArtifactFile

  }
}
package com.sake.build.aether


import io.Source
import java.net.URL
import java.io.{File, FileOutputStream, BufferedOutputStream}


object Resolver {
  def resolver(org: String, module: String, revision: String, artifact: String, transitive: Boolean) {
    val baseURL = "http://repo1.maven.org/maven2/" + "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"
    val resolvedURL = baseURL.replace("[organisation]", org.replace(".", "/")).replace("[module]", module).replace("[revision]", revision).replace("[artifact]", artifact).replace("[ext]", "jar")
    if (transitive) {
      val pomResolvedURL = baseURL.replace("[organisation]", org.replace(".", "/")).replace("[module]", module).replace("[revision]", revision).replace("[artifact]", artifact).replace("[ext]", "pom")
      println("Resolved URL " + pomResolvedURL)
      val pomXML = scala.xml.Source.fromReader(Source.fromURL(pomResolvedURL).reader())
    }
    println("Resolved URL " + resolvedURL)
    val reader = new URL(resolvedURL).openConnection().getInputStream

    val buffer = new Array[Byte](1024)

    val localFile = localFilename(org, module, revision, artifact)
    println("local file = " + localPath(org, module, revision))
    try {
      val created = new File(localPath(org, module, revision)).mkdirs()
      println(created)

    } catch {
      case e: Exception => e.printStackTrace()
    }
    val out = new BufferedOutputStream(new FileOutputStream(localFile))
    Iterator.continually(reader.read(buffer)).takeWhile(_ != -1).foreach(n => out.write(buffer, 0, n))
    out.close()

  }

  val home = System.getProperty("user.home")

  def localPath(org: String, module: String, revision: String) = {

    val baseURL = home+"/.ivy2/cache/" + "[organisation]/[module]/[revision]"
    baseURL.replace("[organisation]", org).replace("[module]", module).replace("[revision]", revision)
  }

  def localFilename(org: String, module: String, revision: String, artifact: String) = {
    val baseURL = home+"/.ivy2/cache/" + "[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"
    baseURL.replace("[organisation]", org).replace("[module]", module).replace("[revision]", revision).replace("[artifact]", artifact).replace("[ext]", "jar")
  }

  /*
  def resolver {
    val repoSystem = newRepositorySystem();

    val session = newSession(repoSystem);


    val dependency = new Dependency(new DefaultArtifact("org.apache.maven:maven-profile:2.2.1"), "compile")
    val central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/")

    val collectRequest = new CollectRequest();
    collectRequest.setRoot(dependency);
    collectRequest.addRepository(central);
    val node = repoSystem.collectDependencies(session, collectRequest).getRoot();

    val dependencyRequest = new DependencyRequest(node, null);

    repoSystem.resolveDependencies(session, dependencyRequest);

    val nlg = new PreorderNodeListGenerator();
    node.accept(nlg);
    System.out.println(nlg.getClassPath());
  }

  def newRepositorySystem = new DefaultPlexusContainer().lookup(classOf[RepositorySystem])

  def newRepositorySystem() = {
    val locator = new DefaultServiceLocator();
    locator.setServices(classOf[WagonProvider], new ManualWagonProvider());
    locator.addService(classOf[RepositoryConnectorFactory], classOf[WagonRepositoryConnectorFactory]);

    return locator.getService(classOf[RepositorySystem]);
  }

  def newSession(system: RepositorySystem) = {
    val session = new MavenRepositorySystemSession();

    val localRepo = new LocalRepository("target/local-repo");
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

    return session;
  }
  */
  def main(args: Array[String]) {
    resolver("org.apache.maven", "maven-profile", "2.2.1", "maven-profile", false)
  }
}

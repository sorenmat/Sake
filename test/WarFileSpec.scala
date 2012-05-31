/**
 * Created by IntelliJ IDEA.
 * User: soren
 * Date: 5/31/12
 * Time: 08:36
 * To change this template use File | Settings | File Templates.
 */

import com.sake.build.Build
import com.sake.build.ivy.JarDependency
import com.sake.build.plugins.Web
import org.specs2.mutable._

class WarFileSpec extends Specification {

val webBuild = new Build with Web  {
  def projectName = "War test build"
  def webRoot = "/Users/soren/workspaces/UnitLinked/Core/SchantzUserManagementWeb/war"
  override def outputDirectory = "/Users/soren/code/buildtest/Foundation/target/classes"
  override def jarDependencies = List(new JarDependency("commons-lang", "commons-lang" , "2.6"))
}

  webBuild.packageWar
  "The 'Hello world' string" should {
    "contain 11 characters" in {
      "Hello world" must have size (11)
    }
    "start with 'Hello'" in {
      "Hello world" must startWith("Hello")
    }
    "end with 'world'" in {
      "Hello world" must endWith("world")
    }
  }


}
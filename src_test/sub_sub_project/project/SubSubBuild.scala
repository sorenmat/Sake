import com.sake.build._
import com.sake.build.ivy._
class SubSubBuild extends Build {

  def projectName = "SubSubTestBuild"

  override def jarDependencies = Set(new JarDependency("org.apache.ivy", "ivy", "2.2.0"))
}
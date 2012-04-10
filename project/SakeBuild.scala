import com.sake.build.Build
import com.sake.build.ivy.JarDependency
class SakeBuild extends Build {

  def projectName = "SakeBuild"
  override def compile {
    println("Compiling Sake...")
    super.compile
  }

  override def jarDependencies = List(new JarDependency("org.apache.ivy", "ivy", "2.2.0"))
}
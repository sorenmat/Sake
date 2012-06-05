import com.sake.build.Build
import com.sake.build.ivy.JarDependency
class SakeBuild extends Build {

  def projectName = "SakeBuild"
//  override def compile {
//    println("Compiling Sake...")
//  }

  override def jarDependencies = Set(new JarDependency("org.apache.ivy", "ivy", "2.2.0"),
		  new JarDependency( "org.scala-lang" , "scala-library" , "2.10.0-M3"),
		  new JarDependency("org.scala-lang" , "scala-compiler" , "2.10.0-M3"))
}
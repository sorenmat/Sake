import com.sake.build.Build
import com.sake.build.ivy.JarDependency
import com.sake.build.plugins._

class SakeBuild extends Build with ScalaDoc {

  def projectName = "SakeBuild"


  override def jarDependencies = Set(new JarDependency("org.apache.ivy", "ivy", "2.2.0"),
		  new JarDependency( "org.scala-lang" , "scala-library" , "2.10.0-M3"),
		  new JarDependency("org.scala-lang" , "scala-compiler" , "2.10.0-M3"))
}
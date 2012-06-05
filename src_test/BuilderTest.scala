import com.sake.build.Build
import java.io.File

object BuilderTest {
  def main(args: Array[String]) {
    val build = new Build {
      def projectName: String = "testing"
      override def outputDirectory = "/Users/soren/workspaces/UnitLinked/Foundation/target/classes"
      override def toInclude(classDir: String) = {
        val inc = super.toInclude(outputDirectory)
        val resourceFiles = recursiveListFiles(new File("/Users/soren/workspaces/UnitLinked/Foundation/resources")).map(f => (f.getCanonicalPath, f.getCanonicalPath.replace(new File("/Users/soren/workspaces/UnitLinked/Foundation/resources").getCanonicalPath, ""))).toList
        println("Filesw: " + (inc ++ resourceFiles).mkString("\n\t"))
        inc ++ resourceFiles
      }
    }
    //build.packageJar
    println(new File(".").getCanonicalPath)
    //build.makeJar
  }

}

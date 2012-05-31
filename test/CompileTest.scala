import com.sake.build.Sake._
import com.sake.build.Sake
object CompileTest {
  def main(args: Array[String]) {
    Sake.main(Array("compile", "src_test/sub_project"))
//	  runTargetOnBuild("compile", "src_test/sub_project")
  }
}
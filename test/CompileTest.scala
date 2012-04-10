import com.sake.build.Sake._
object CompileTest {
  def main(args: Array[String]) {
	  runTargetOnBuild("compile", "src_test/sub_project")
  }
}
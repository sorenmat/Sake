import com.sake.build.SubProject
import com.sake.build.Build
class TestBuild extends Build {

  def projectName = "TestBuild"

  override def subProjects = new SubProject("src_test/sub_project") :: Nil
}
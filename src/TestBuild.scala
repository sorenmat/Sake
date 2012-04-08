import build.Build
import build.SubProject
class TestBuild extends Build {

  def projectName = "TestBuild"

  override def subProjects = SubProject("../src_test/sub_project") :: Nil
}
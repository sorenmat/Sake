import build._
class SubBuild extends Build {

  def projectName = "SubTestBuild"

  override def subProjects = SubProject("src_test/sub_sub_project") :: Nil
}
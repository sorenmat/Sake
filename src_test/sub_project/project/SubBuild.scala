import com.sake.build._

class SubBuild extends Build {

  def projectName = "SubTestBuild"

  //override def subProjects = new SubProject("src_test/sub_sub_project") :: Nil
}
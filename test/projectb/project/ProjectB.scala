import com.sake.build._

class ProjectB extends Build {
	def projectName = "ProjectB"

override def subProjects = List(new SubProject("../projecta"))
}
import com.sake.build._

class ProjectC extends Build {
	def projectName = "ProjectC"

	override def subProjects = List(new SubProject("../projectb"))
}
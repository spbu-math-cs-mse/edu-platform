import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.*

class ParentCore(
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val solutionDistributor: SolutionDistributor,
) {
  fun getChildren(parentId: ParentId): List<Student> = studentStorage.getStudents(parentId)

  fun getStudentPerformance(studentId: StudentId) = gradeTable.getStudentPerformance(studentId, solutionDistributor)
}

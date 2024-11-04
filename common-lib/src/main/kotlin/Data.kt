@file:Suppress("unused")

data class Student(
  val id: String,
)

data class Parent(
  val id: String,
  val children: List<Student>,
)

data class Teacher(
  val id: String,
)

data class Problem(
  val id: String,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  PHOTOS,
  DOCUMENT,
}

data class Solution(
  val id: String,
  val problem: Problem,
  val content: SolutionContent,
  val type: SolutionType,
)
typealias Grade = Int

class Course(
  val teachers: MutableList<Teacher>,
  val students: MutableList<Student>,
  var description: String,
  val gradeTable: GradeTable,
)

data class SolutionContent(
  val fileIds: List<String>? = null,
  val text: String? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comments: String,
)

interface GradeTable {
  fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  )

  fun getGradeMap(): Map<Student, Map<Problem, Grade>>
}

interface SolutionDistributor {
  fun inputSolution(
    student: Student,
    solutionContent: SolutionContent,
  ): Solution

  fun querySolution(teacher: Teacher): Pair<Solution, SolutionContent>

  fun assessSolution(
    solution: Solution,
    teacher: Teacher,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
  )
}

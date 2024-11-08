class TeacherCore(
  private val solutionDistributor: SolutionDistributor,
  private val usernamesRegistry: UsernamesRegistry,
) : UsernamesRegistry by usernamesRegistry {
  fun querySolution(username: String): Pair<Solution, SolutionContent> =
    solutionDistributor.querySolution(Teacher(getUserId(username)))

  fun assessSolution(
    solution: Solution,
    username: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
  ) {
    solutionDistributor.assessSolution(
      solution,
      Teacher(getUserId(username)),
      assessment,
      gradeTable,
    )
  }
}

interface UsernamesRegistry {
  fun getUserId(username: String): String

  fun setUserId(
    username: String,
    id: String,
  )
}

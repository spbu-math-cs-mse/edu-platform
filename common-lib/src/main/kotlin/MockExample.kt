class MockGradeTable(
    val constGradeMap: Map<Student, Map<Problem, Int>> = mapOf(
        Student("1") to mapOf(Problem("1c") to 100500)
    )
) : GradeTable {
    override fun addAssessment(
        student: Student,
        teacher: Teacher,
        solution: Solution,
        assessment: SolutionAssessment
    ) {
    }

    override fun getGradeMap(): Map<Student, Map<Problem, Grade>> =
        constGradeMap

}
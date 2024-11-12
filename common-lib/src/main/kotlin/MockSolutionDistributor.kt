package com.github.heheteam.commonlib

class MockSolutionDistributor : SolutionDistributor {
  override fun inputSolution(
    studentId: String,
    solutionContent: SolutionContent,
  ): Solution {
    TODO("Not yet implemented")
  }

  override fun querySolution(teacherId: String): Solution? {
    TODO("Not yet implemented")
  }

  override fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
  ) {
    TODO("Not yet implemented")
  }
}

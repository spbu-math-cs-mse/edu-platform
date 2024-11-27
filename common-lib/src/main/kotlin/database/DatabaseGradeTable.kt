package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.database.tables.AssessmentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert

class DatabaseGradeTable(
  val database: Database,
) : GradeTable {
  override fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  ) {
    AssessmentTable.insert {
      it[AssessmentTable.solutionId] = solution.id
      it[AssessmentTable.teacherId] = teacher.id
      it[AssessmentTable.grade] = assessment.grade
    }
  }

  override fun getStudentPerformance(studentId: Long): Map<Problem, Grade> {
    TODO("Not yet implemented")
  }
}

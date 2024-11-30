package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssessmentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class DatabaseGradeTable(
  val database: Database,
) : GradeTable {
  init {
    transaction(database) {
      SchemaUtils.create(AssessmentTable)
    }
  }
  override fun addAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    AssessmentTable.insert {
      it[AssessmentTable.solutionId] = solutionId.id
      it[AssessmentTable.teacherId] = teacherId.id
      it[AssessmentTable.grade] = assessment.grade
    }
  }

  override fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> {
    val ids = transaction(database) {
      AssessmentTable.selectAll()
        .map { it[AssessmentTable.solutionId].value to it[AssessmentTable.grade] }
    }
    return ids.mapNotNull { (solutionId, grade) ->
      val solution =
        solutionDistributor.resolveSolution(solutionId.toSolutionId())
      if (solution.studentId != studentId) null else solution.problemId to grade
    }.toMap()
  }

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime,
  ) {
    transaction(database) {
      AssessmentTable.insert {
        it[AssessmentTable.solutionId] = solutionId.id
        it[AssessmentTable.teacherId] = teacherId.id
        it[AssessmentTable.grade] = assessment.grade
        it[AssessmentTable.comment] = assessment.comment
      }
    }
  }

  override fun isChecked(solutionId: SolutionId): Boolean =
    !transaction(database) {
      AssessmentTable.selectAll()
        .where(AssessmentTable.solutionId eq solutionId.id)
        .empty()
    }
}

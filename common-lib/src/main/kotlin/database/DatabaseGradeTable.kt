package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.ProblemState
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssessmentTable
import com.github.heheteam.commonlib.database.tables.ProblemTable
import com.github.heheteam.commonlib.database.tables.SolutionTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
  ): Map<ProblemId, ProblemState> =
    transaction(database) {
      AssessmentTable
        .join(
          SolutionTable,
          JoinType.RIGHT,
          onColumn = AssessmentTable.solutionId,
          otherColumn = SolutionTable.id,
        ).join(ProblemTable, JoinType.INNER, onColumn = SolutionTable.problemId, otherColumn = ProblemTable.id)
        .selectAll()
        .where { SolutionTable.studentId eq studentId.id }
        .associate { it[SolutionTable.problemId].value.toProblemId() to
          ProblemState(
            it[AssessmentTable.grade],
            //true
            isChecked(SolutionId(it[SolutionTable.id].value)),
          )
        }
    }

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: AssignmentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, ProblemState> =
    transaction(database) {
      AssessmentTable
        .join(
          SolutionTable,
          JoinType.RIGHT,onColumn = AssessmentTable.solutionId,
          otherColumn = SolutionTable.id,
        ).join(ProblemTable, JoinType.INNER, onColumn = SolutionTable.problemId, otherColumn = ProblemTable.id)
        .selectAll()
        .where {
          (SolutionTable.studentId eq studentId.id) and
            (ProblemTable.assignmentId eq assignmentId.id)
        }.associate {
          it[SolutionTable.problemId].value.toProblemId() to
            ProblemState(
              it[AssessmentTable.grade],
              isChecked(SolutionId(it[SolutionTable.id].value)),
            )
        }
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
      AssessmentTable
        .selectAll()
        .where(AssessmentTable.solutionId eq solutionId.id)
        .empty()
    }
}

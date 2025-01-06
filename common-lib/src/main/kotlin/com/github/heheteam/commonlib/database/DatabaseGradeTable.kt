package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.toProblemId
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SolutionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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

  override fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> =
    transaction(database) {
      AssessmentTable
        .join(
          SolutionTable,
          JoinType.INNER,
          onColumn = AssessmentTable.solutionId,
          otherColumn = SolutionTable.id,
        ).join(ProblemTable, JoinType.INNER, onColumn = SolutionTable.problemId, otherColumn = ProblemTable.id)
        .selectAll()
        .where { SolutionTable.studentId eq studentId.id and AssessmentTable.grade.isNotNull() }
        .associate { it[SolutionTable.problemId].value.toProblemId() to it[AssessmentTable.grade] }
    }

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: List<AssignmentId>,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> =
    transaction(database) {
      AssessmentTable
        .join(
          SolutionTable,
          JoinType.INNER,
          onColumn = AssessmentTable.solutionId,
          otherColumn = SolutionTable.id,
        ).join(ProblemTable, JoinType.INNER, onColumn = SolutionTable.problemId, otherColumn = ProblemTable.id)
        .selectAll()
        .where {
          (SolutionTable.studentId eq studentId.id) and
            (ProblemTable.assignmentId inList assignmentId.map { it.id }) and
            AssessmentTable.grade.isNotNull()
        }.associate { it[SolutionTable.problemId].value.toProblemId() to it[AssessmentTable.grade] }
    }

  override fun getCourseRating(
    courseId: CourseId,
    solutionDistributor: SolutionDistributor,
  ): Map<StudentId, Map<ProblemId, Grade>> = transaction(database) {
    AssessmentTable
      .join(
        SolutionTable,
        JoinType.INNER,
        onColumn = AssessmentTable.solutionId,
        otherColumn = SolutionTable.id,
      ).join(ProblemTable, JoinType.INNER, onColumn = SolutionTable.problemId, otherColumn = ProblemTable.id)
      .join(
        AssignmentTable,
        JoinType.INNER,
        onColumn = ProblemTable.assignmentId,
        otherColumn = AssignmentTable.id
      )
      .selectAll()
      .where {
        (AssignmentTable.courseId eq courseId.id) and
          AssessmentTable.grade.isNotNull()
      }.groupBy { it[SolutionTable.studentId].value.toStudentId() }
      .mapValues { (_, trios) -> // Transform each group into a Map
        trios.associate { it[ProblemTable.id].value.toProblemId() to it[AssessmentTable.grade] }
      }
  }

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
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

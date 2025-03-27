package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.ProblemGrade
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.toAssignmentId
import com.github.heheteam.commonlib.api.toProblemId
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SolutionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseGradeTable(val database: Database) : GradeTable {
  init {
    transaction(database) { SchemaUtils.create(AssessmentTable) }
  }

  override fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade?> =
    transaction(database) {
      SolutionTable.join(
          AssessmentTable,
          JoinType.LEFT,
          onColumn = SolutionTable.id,
          otherColumn = AssessmentTable.solutionId,
        )
        .join(
          ProblemTable,
          JoinType.INNER,
          onColumn = SolutionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .selectAll()
        .where { SolutionTable.studentId eq studentId.id }
        .orderBy(
          AssessmentTable.timestamp,
          SortOrder.ASC,
        ) // associate takes the latter entry with the same key
        .associate { it[SolutionTable.problemId].value.toProblemId() to it[AssessmentTable.grade] }
    }

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: AssignmentId,
  ): List<Pair<Problem, ProblemGrade>> =
    transaction(database) {
      SolutionTable.join(
          AssessmentTable,
          JoinType.LEFT,
          onColumn = SolutionTable.id,
          otherColumn = AssessmentTable.solutionId,
        )
        .join(
          ProblemTable,
          JoinType.RIGHT,
          onColumn = SolutionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .selectAll()
        .where {
          ((SolutionTable.studentId eq studentId.id) or (SolutionTable.id eq null)) and
            (ProblemTable.assignmentId eq assignmentId.id)
        }
        .orderBy(
          SolutionTable.timestamp to SortOrder.ASC,
          AssessmentTable.timestamp to SortOrder.ASC,
        )
        .associate {
          Problem(
            it[ProblemTable.id].value.toProblemId(),
            it[ProblemTable.serialNumber],
            it[ProblemTable.number],
            it[ProblemTable.description],
            it[ProblemTable.maxScore],
            it[ProblemTable.assignmentId].value.toAssignmentId(),
            it[ProblemTable.deadline],
          ) to
            if (it.getOrNull(AssessmentTable.grade) != null) {
              ProblemGrade.Graded(it[AssessmentTable.grade])
            } else if (it.getOrNull(SolutionTable.id) != null) {
              ProblemGrade.Unchecked()
            } else {
              ProblemGrade.Unsent()
            }
        }
        .toList()
        .sortedBy { it.first.serialNumber }
        .map { it.first to it.second }
    }

  override fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>> =
    transaction(database) {
      SolutionTable.join(
          AssessmentTable,
          JoinType.LEFT,
          onColumn = SolutionTable.id,
          otherColumn = AssessmentTable.solutionId,
        )
        .join(
          ProblemTable,
          JoinType.INNER,
          onColumn = SolutionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where { AssignmentTable.courseId eq courseId.id }
        .groupBy { it[SolutionTable.studentId].value.toStudentId() }
        .mapValues { (_, trios) -> // Transform each group into a Map
          trios.associate { it[ProblemTable.id].value.toProblemId() to it[AssessmentTable.grade] }
        }
    }

  override fun recordSolutionAssessment(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: kotlinx.datetime.LocalDateTime,
  ) {
    transaction(database) {
      AssessmentTable.insert {
        it[AssessmentTable.solutionId] = solutionId.id
        it[AssessmentTable.teacherId] = teacherId.id
        it[AssessmentTable.grade] = assessment.grade
        it[AssessmentTable.comment] = assessment.comment
        it[AssessmentTable.timestamp] = timestamp
      }
    }
  }

  override fun isChecked(solutionId: SolutionId): Boolean =
    !transaction(database) {
      AssessmentTable.selectAll().where(AssessmentTable.solutionId eq solutionId.id).empty()
    }

  override fun getGradingsForSolution(solutionId: SolutionId): List<GradingEntry> =
    transaction(database) {
      AssessmentTable.selectAll().where(AssessmentTable.solutionId eq solutionId.id).map {
        GradingEntry(
          it[AssessmentTable.teacherId].value.toTeacherId(),
          SolutionAssessment(it[AssessmentTable.grade], it[AssessmentTable.comment]),
          it[AssessmentTable.timestamp],
        )
      }
    }
}

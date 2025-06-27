package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SubmissionTable
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toAssignmentId
import com.github.heheteam.commonlib.interfaces.toProblemId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
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

  override fun getStudentPerformance(
    studentId: StudentId
  ): Result<Map<ProblemId, Grade?>, EduPlatformError> =
    catchingTransaction(database) {
      SubmissionTable.join(
          AssessmentTable,
          JoinType.LEFT,
          onColumn = SubmissionTable.id,
          otherColumn = AssessmentTable.submissionId,
        )
        .join(
          ProblemTable,
          JoinType.INNER,
          onColumn = SubmissionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .selectAll()
        .where { SubmissionTable.studentId eq studentId.long }
        .orderBy(
          AssessmentTable.timestamp,
          SortOrder.ASC,
        ) // associate takes the latter entry with the same key
        .associate {
          it[SubmissionTable.problemId].value.toProblemId() to it[AssessmentTable.grade]
        }
    }

  override fun getStudentPerformance(
    studentId: StudentId,
    assignmentId: AssignmentId,
  ): Result<List<Pair<Problem, ProblemGrade>>, EduPlatformError> =
    catchingTransaction(database) {
      SubmissionTable.join(
          AssessmentTable,
          JoinType.LEFT,
          onColumn = SubmissionTable.id,
          otherColumn = AssessmentTable.submissionId,
        )
        .join(
          ProblemTable,
          JoinType.RIGHT,
          onColumn = SubmissionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .selectAll()
        .where {
          ((SubmissionTable.studentId eq studentId.long) or (SubmissionTable.id eq null)) and
            (ProblemTable.assignmentId eq assignmentId.long)
        }
        .orderBy(
          SubmissionTable.timestamp to SortOrder.ASC,
          AssessmentTable.timestamp to SortOrder.ASC,
        )
        .associate {
          createProblemFromDatabaseRow(it) to
            if (it.getOrNull(AssessmentTable.grade) != null) {
              ProblemGrade.Graded(it[AssessmentTable.grade])
            } else if (it.getOrNull(SubmissionTable.id) != null) {
              ProblemGrade.Unchecked
            } else {
              ProblemGrade.Unsent
            }
        }
        .toList()
        .sortedBy { it.first.serialNumber }
        .map { it.first to it.second }
    }

  override fun getCourseRating(
    courseId: CourseId
  ): Result<Map<StudentId, Map<ProblemId, Grade?>>, EduPlatformError> =
    catchingTransaction(database) {
      SubmissionTable.join(
          AssessmentTable,
          JoinType.LEFT,
          onColumn = SubmissionTable.id,
          otherColumn = AssessmentTable.submissionId,
        )
        .join(
          ProblemTable,
          JoinType.INNER,
          onColumn = SubmissionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where { AssignmentTable.courseId eq courseId.long }
        .orderBy(
          SubmissionTable.timestamp to SortOrder.ASC,
          AssessmentTable.timestamp to SortOrder.ASC,
        )
        .groupBy { it[SubmissionTable.studentId].value.toStudentId() }
        .mapValues { (_, trios) -> // Transform each group into a Map
          trios.associate { it[ProblemTable.id].value.toProblemId() to it[AssessmentTable.grade] }
        }
    }

  override fun recordSubmissionAssessment(
    submissionId: SubmissionId,
    teacherId: TeacherId,
    assessment: SubmissionAssessment,
    timestamp: LocalDateTime,
  ): Result<Unit, EduPlatformError> =
    catchingTransaction(database) {
      AssessmentTable.insert {
        it[AssessmentTable.submissionId] = submissionId.long
        it[AssessmentTable.teacherId] = teacherId.long
        it[AssessmentTable.grade] = assessment.grade
        it[AssessmentTable.comment] = assessment.comment
        it[AssessmentTable.timestamp] = timestamp
      }
    }

  override fun getGradingsForSubmission(
    submissionId: SubmissionId
  ): Result<List<GradingEntry>, EduPlatformError> =
    catchingTransaction(database) {
      AssessmentTable.selectAll().where(AssessmentTable.submissionId eq submissionId.long).map {
        GradingEntry(
          it[AssessmentTable.teacherId].value.toTeacherId(),
          SubmissionAssessment(it[AssessmentTable.grade], it[AssessmentTable.comment]),
          it[AssessmentTable.timestamp],
        )
      }
    }
}

private fun createProblemFromDatabaseRow(row: ResultRow): Problem =
  Problem(
    row[ProblemTable.id].value.toProblemId(),
    row[ProblemTable.serialNumber],
    row[ProblemTable.number],
    row[ProblemTable.description],
    row[ProblemTable.maxScore],
    row[ProblemTable.assignmentId].value.toAssignmentId(),
    row[ProblemTable.deadline],
  )

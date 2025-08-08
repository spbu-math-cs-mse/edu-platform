package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.ChallengeAccessTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toAssignmentId
import com.github.heheteam.commonlib.interfaces.toCourseId
import com.github.heheteam.commonlib.interfaces.toProblemId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseProblemStorage(val database: Database) : ProblemStorage {
  init {
    transaction(database) { SchemaUtils.create(ProblemTable) }
  }

  override fun resolveProblem(problemId: ProblemId): Result<Problem, ResolveError<ProblemId>> {
    val row =
      transaction(database) {
        ProblemTable.selectAll().where(ProblemTable.id eq problemId.long).singleOrNull()
      } ?: return Err(ResolveError(problemId))
    return Ok(
      Problem(
        problemId,
        row[ProblemTable.serialNumber],
        row[ProblemTable.number],
        row[ProblemTable.description],
        row[ProblemTable.maxScore],
        row[ProblemTable.assignmentId].value.toAssignmentId(),
      )
    )
  }

  override fun createProblem(
    assignmentId: AssignmentId,
    serialNumber: Int,
    problemDescription: ProblemDescription,
  ): Result<ProblemId, EduPlatformError> =
    catchingTransaction(database) {
        ProblemTable.insertAndGetId {
          it[ProblemTable.serialNumber] = serialNumber
          it[ProblemTable.number] = problemDescription.number
          it[ProblemTable.assignmentId] = assignmentId.long
          it[ProblemTable.maxScore] = problemDescription.maxScore
          it[ProblemTable.description] = problemDescription.description
          it[ProblemTable.deadline] = problemDescription.deadline
        }
      }
      .map { it.value.toProblemId() }

  override fun getProblemsFromAssignment(
    assignmentId: AssignmentId
  ): Result<List<Problem>, EduPlatformError> =
    catchingTransaction(database) {
      ProblemTable.selectAll().where(ProblemTable.assignmentId eq assignmentId.long).map {
        Problem(
          it[ProblemTable.id].value.toProblemId(),
          it[ProblemTable.serialNumber],
          it[ProblemTable.number],
          it[ProblemTable.description],
          it[ProblemTable.maxScore],
          it[ProblemTable.assignmentId].value.toAssignmentId(),
          it[ProblemTable.deadline],
        )
      }
    }

  override fun getProblemsFromCourse(courseId: CourseId): Result<List<Problem>, EduPlatformError> =
    catchingTransaction(database) {
      ProblemTable.join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where(AssignmentTable.courseId eq courseId.long)
        .map {
          Problem(
            it[ProblemTable.id].value.toProblemId(),
            it[ProblemTable.serialNumber],
            it[ProblemTable.number],
            it[ProblemTable.description],
            it[ProblemTable.maxScore],
            it[ProblemTable.assignmentId].value.toAssignmentId(),
            it[ProblemTable.deadline],
          )
        }
    }

  @Suppress("LongMethod")
  override fun getProblemsWithAssignmentsFromCourse(
    courseId: CourseId,
    studentId: StudentId,
  ): Result<Map<Assignment, List<Problem>>, EduPlatformError> =
    catchingTransaction(database) {
      ProblemTable.join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where {
          (AssignmentTable.courseId eq courseId.long) and
            ((AssignmentTable.isChallenge eq false) or
              ((AssignmentTable.isChallenge eq true) and
                (exists(
                  ChallengeAccessTable.selectAll().where {
                    (ChallengeAccessTable.challengeId eq AssignmentTable.id) and
                      (ChallengeAccessTable.studentId eq studentId.long)
                  }
                ))))
        }
        .groupBy({
          Assignment(
            it[AssignmentTable.id].value.toAssignmentId(),
            it[AssignmentTable.serialNumber],
            it[AssignmentTable.description],
            it[AssignmentTable.courseId].value.toCourseId(),
            it[AssignmentTable.statementsUrl],
            it[AssignmentTable.challengeId]?.value?.toAssignmentId(),
          )
        }) {
          Problem(
            it[ProblemTable.id].value.toProblemId(),
            it[ProblemTable.serialNumber],
            it[ProblemTable.number],
            it[ProblemTable.description],
            it[ProblemTable.maxScore],
            it[ProblemTable.assignmentId].value.toAssignmentId(),
            it[ProblemTable.deadline],
          )
        }
    }
}

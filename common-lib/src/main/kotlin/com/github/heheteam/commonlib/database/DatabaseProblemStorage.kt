package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ResolveError
import com.github.heheteam.commonlib.api.toAssignmentId
import com.github.heheteam.commonlib.api.toCourseId
import com.github.heheteam.commonlib.api.toProblemId
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseProblemStorage(val database: Database) : ProblemStorage {
  init {
    transaction(database) { SchemaUtils.create(ProblemTable) }
  }

  override fun resolveProblem(problemId: ProblemId): Result<Problem, ResolveError<ProblemId>> {
    val row =
      transaction(database) {
        ProblemTable.selectAll().where(ProblemTable.id eq problemId.id).singleOrNull()
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
    number: String,
    maxScore: Grade,
    description: String,
    deadline: LocalDateTime?,
  ): ProblemId =
    transaction(database) {
        ProblemTable.insertAndGetId {
          it[ProblemTable.serialNumber] = serialNumber
          it[ProblemTable.number] = number
          it[ProblemTable.assignmentId] = assignmentId.id
          it[ProblemTable.maxScore] = maxScore
          it[ProblemTable.description] = description
          it[ProblemTable.deadline] = deadline
        }
      }
      .value
      .toProblemId()

  override fun getProblemsFromAssignment(assignmentId: AssignmentId): List<Problem> =
    transaction(database) {
      ProblemTable.selectAll().where(ProblemTable.assignmentId eq assignmentId.id).map {
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

  override fun getProblemsFromCourse(courseId: CourseId): List<Problem> =
    transaction(database) {
      ProblemTable.join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where(AssignmentTable.courseId eq courseId.id)
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

  override fun getProblemsWithAssignmentsFromCourse(
    courseId: CourseId
  ): Map<Assignment, List<Problem>> =
    transaction(database) {
      ProblemTable.join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where(AssignmentTable.courseId eq courseId.id)
        .groupBy({
          Assignment(
            it[AssignmentTable.id].value.toAssignmentId(),
            it[AssignmentTable.serialNumber],
            it[AssignmentTable.description],
            it[AssignmentTable.courseId].value.toCourseId(),
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

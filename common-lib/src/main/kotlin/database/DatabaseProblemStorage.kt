package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssignmentTable
import com.github.heheteam.commonlib.database.tables.ProblemTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseProblemStorage(
  val database: Database,
) : ProblemStorage {
  init {
    transaction(database) {
      SchemaUtils.create(ProblemTable)
    }
  }

  override fun resolveProblem(problemId: ProblemId): Result<Problem, ResolveError<ProblemId>> {
    val row =
      transaction(database) {
        ProblemTable
          .selectAll()
          .where(ProblemTable.id eq problemId.id)
          .singleOrNull()
      } ?: return Err(ResolveError(problemId))
    return Ok(
      Problem(
        problemId,
        row[ProblemTable.number],
        row[ProblemTable.description],
        row[ProblemTable.maxScore],
        row[ProblemTable.assignmentId].value.toAssignmentId(),
      ),
    )
  }

  override fun createProblem(
    assignmentId: AssignmentId,
    number: String,
    maxScore: Grade,
    description: String,
  ): ProblemId =
    transaction(database) {
      ProblemTable.insertAndGetId {
        it[ProblemTable.number] = number
        it[ProblemTable.assignmentId] = assignmentId.id
        it[ProblemTable.maxScore] = maxScore
        it[ProblemTable.description] = description
      }
    }.value.toProblemId()

  override fun getProblemsFromAssignment(assignmentId: AssignmentId): List<Problem> =
    transaction(database) {
      ProblemTable
        .selectAll()
        .where(ProblemTable.assignmentId eq assignmentId.id)
        .map {
          Problem(
            it[ProblemTable.id].value.toProblemId(),
            it[ProblemTable.number],
            it[ProblemTable.description],
            it[ProblemTable.maxScore],
            it[ProblemTable.assignmentId].value.toAssignmentId(),
          )
        }
    }

  override fun getProblemsFromCourse(courseId: CourseId): List<Problem> = transaction(database) {
    ProblemTable
      .join(AssignmentTable, JoinType.INNER, onColumn = ProblemTable.assignmentId, otherColumn = AssignmentTable.id)
      .selectAll()
      .where(AssignmentTable.courseId eq courseId.id)
      .map {
        Problem(
          it[ProblemTable.id].value.toProblemId(),
          it[ProblemTable.number],
          it[ProblemTable.description],
          it[ProblemTable.maxScore],
          it[ProblemTable.assignmentId].value.toAssignmentId(),
        )
      }
  }
}

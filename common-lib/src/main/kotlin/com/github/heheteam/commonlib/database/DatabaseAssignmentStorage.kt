package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ResolveError
import com.github.heheteam.commonlib.api.toAssignmentId
import com.github.heheteam.commonlib.api.toCourseId
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAssignmentStorage(val database: Database) : AssignmentStorage {
  init {
    transaction(database) { SchemaUtils.create(AssignmentTable) }
  }

  override fun resolveAssignment(
    assignmentId: AssignmentId
  ): Result<Assignment, ResolveError<AssignmentId>> {
    val row =
      transaction {
        AssignmentTable.selectAll().where(AssignmentTable.id eq assignmentId.id).singleOrNull()
      } ?: return Err(ResolveError(assignmentId))
    return Ok(
      Assignment(
        assignmentId,
        row[AssignmentTable.serialNumber],
        row[AssignmentTable.description],
        row[AssignmentTable.courseId].value.toCourseId(),
      )
    )
  }

  override fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
    problemStorage: ProblemStorage,
  ): AssignmentId {
    val assignId =
      transaction(database) {
          val serialNumber =
            (AssignmentTable.select(AssignmentTable.serialNumber.max())
              .where(AssignmentTable.courseId eq courseId.id)
              .firstOrNull()
              ?.get(AssignmentTable.serialNumber.max()) ?: 0) + 1
          AssignmentTable.insertAndGetId {
            it[AssignmentTable.serialNumber] = serialNumber
            it[AssignmentTable.description] = description
            it[AssignmentTable.courseId] = courseId.id
          }
        }
        .value
        .toAssignmentId()
    problemsDescriptions.mapIndexed { number, problemDescription ->
      problemStorage.createProblem(
        assignId,
        number,
        problemDescription.number,
        problemDescription.maxScore,
        problemDescription.description,
        problemDescription.deadline,
      )
    }
    return assignId
  }

  override fun getAssignmentsForCourse(courseId: CourseId): List<Assignment> = transaction {
    AssignmentTable.selectAll().where(AssignmentTable.courseId eq courseId.id).map {
      Assignment(
        it[AssignmentTable.id].value.toAssignmentId(),
        it[AssignmentTable.serialNumber],
        it[AssignmentTable.description],
        it[AssignmentTable.courseId].value.toCourseId(),
      )
    }
  }
}

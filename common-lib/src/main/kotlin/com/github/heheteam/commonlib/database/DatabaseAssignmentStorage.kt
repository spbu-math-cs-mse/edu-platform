package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.toAssignmentId
import com.github.heheteam.commonlib.interfaces.toCourseId
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

class DatabaseAssignmentStorage(
  val database: Database,
  private val problemStorage: ProblemStorage,
) : AssignmentStorage {
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

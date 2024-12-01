package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssignmentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAssignmentStorage(
  val database: Database,
) : AssignmentStorage {
  init {
    transaction(database) { SchemaUtils.create(AssignmentTable) }
  }

  override fun resolveAssignment(assignmentId: AssignmentId): Assignment {
    val row =
      transaction {
        AssignmentTable
          .selectAll()
          .where(
            AssignmentTable.id eq assignmentId.id,
          ).single()
      }
    return Assignment(
      assignmentId,
      row[AssignmentTable.description],
      row[AssignmentTable.course].value.toCourseId(),
    )
  }

  override fun createAssignment(
    courseId: CourseId,
    description: String,
    problemNames: List<String>,
    problemStorage: ProblemStorage,
  ): AssignmentId {
    val assignId =
      transaction(database) {
        AssignmentTable.insertAndGetId {
          it[AssignmentTable.description] = description
          it[AssignmentTable.course] = courseId.id
        }
      }.value.toAssignmentId()
    problemNames
      .map { problemStorage.createProblem(assignId, it) }
    return assignId
  }

  override fun getAssignmentsForCourse(courseId: CourseId): List<AssignmentId> =
    transaction {
      AssignmentTable
        .selectAll()
        .where(
          AssignmentTable.course eq courseId.id,
        ).map { it[AssignmentTable.id].value.toAssignmentId() }
    }
}

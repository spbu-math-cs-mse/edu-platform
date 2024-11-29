package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssignmentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAssignmentStorage(val database: Database) : AssignmentStorage {
  init {
    transaction(database) { SchemaUtils.create(AssignmentTable) }
  }

  override fun resolveAssignment(assignmentId: AssignmentId): Assignment {
    TODO("Not yet implemented")
  }

  override fun createAssignment(
    courseId: CourseId,
    description: String,
    problemNames: List<String>,
    problemStorage: ProblemStorage,
  ): AssignmentId {
    val assgnId = transaction(database) {
      AssignmentTable.insertAndGetId {
        it[AssignmentTable.description] = description
        it[AssignmentTable.course] = courseId.id
      }
    }.value.toAssignmentId()
    problemNames
      .map { problemStorage.createProblem(assgnId, it) }
    return assgnId
  }

  override fun getAssignmentsForCourse(courseId: CourseId): List<AssignmentId> {
    TODO("Not yet implemented")
  }
}

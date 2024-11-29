package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.ProblemTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseProblemStorage(val database: Database) : ProblemStorage {
  init {
    transaction(database) {
      SchemaUtils.create(ProblemTable)
    }
  }

  override fun resolveProblem(id: ProblemId): Problem {
    val row = transaction(database) {
      ProblemTable.selectAll().where(ProblemTable.id eq id.id)
        .single()
    }
    return Problem(
      id,
      id.toString(),
      "",
      1,
      row[ProblemTable.assignmentId].value.toAssignmentId()
    )
  }

  override fun createProblem(
    assignmentId: AssignmentId,
    number: String
  ): ProblemId {
    return transaction(database) {
      ProblemTable.insertAndGetId {
        it[ProblemTable.number] = "1"
        it[ProblemTable.assignmentId] = assignmentId.id
        it[ProblemTable.maxScore] = 1
        it[ProblemTable.description] = ""
      }
    }.value.toProblemId()
  }

  override fun getProblemsFromAssignment(id: AssignmentId): List<ProblemId> =
    transaction(database) {
      ProblemTable.selectAll().where(ProblemTable.assignmentId eq id.id)
    }.map { it[ProblemTable.id].value.toProblemId() }
}
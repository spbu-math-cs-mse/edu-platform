package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssignmentTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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

    override fun resolveAssignment(assignmentId: AssignmentId): Result<Assignment, ResolveError<AssignmentId>> {
        val row =
            transaction {
                AssignmentTable
                    .selectAll()
                    .where(
                        AssignmentTable.id eq assignmentId.id,
                    ).singleOrNull()
            } ?: return Err(ResolveError(assignmentId))
        return Ok(
            Assignment(
                assignmentId,
                row[AssignmentTable.description],
                row[AssignmentTable.courseId].value.toCourseId(),
            ),
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
                    it[AssignmentTable.courseId] = courseId.id
                }
            }.value.toAssignmentId()
        problemNames
            .map { problemStorage.createProblem(assignId, it, 1, "") } // TODO
        return assignId
    }

    override fun getAssignmentsForCourse(courseId: CourseId): List<Assignment> =
        transaction {
            AssignmentTable
                .selectAll()
                .where(
                    AssignmentTable.courseId eq courseId.id,
                ).map {
                    Assignment(
                        it[AssignmentTable.id].value.toAssignmentId(),
                        it[AssignmentTable.description],
                        it[AssignmentTable.courseId].value.toCourseId(),
                    )
                }
        }
}

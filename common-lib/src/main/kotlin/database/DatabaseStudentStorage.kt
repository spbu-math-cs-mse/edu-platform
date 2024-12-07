package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.ParentStudents
import com.github.heheteam.commonlib.database.tables.StudentTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseStudentStorage(
  val database: Database,
) : StudentStorage {
  init {
    transaction(database) {
      SchemaUtils.create(StudentTable)
      SchemaUtils.create(ParentStudents)
    }
  }

  override fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ): Result<Unit, BindError<StudentId, ParentId>> =
    try {
      transaction(database) {
        ParentStudents.insert {
          it[ParentStudents.studentId] = studentId.id
          it[ParentStudents.parentId] = parentId.id
        }
      }
      Ok(Unit)
    } catch (e: Throwable) {
      Err(BindError(studentId, parentId))
    }

  override fun getChildren(parentId: ParentId): List<Student> {
    val ids =
      transaction(database) {
        ParentStudents
          .selectAll()
          .where(ParentStudents.parentId eq parentId.id)
      }.map { it[ParentStudents.studentId].value }
    return transaction(database) {
      return@transaction ids.map { studentId ->
        StudentTable
          .selectAll()
          .where(StudentTable.id eq studentId)
          .map {
            Student(
              studentId.toStudentId(),
              it[StudentTable.name],
              it[StudentTable.name],
            )
          }.single()
      }
    }
  }

  override fun createStudent(): StudentId =
    transaction(database) {
      StudentTable.insert {
        it[StudentTable.name] = "defaultName"
        it[StudentTable.surname] = "defaultSurname"
        it[StudentTable.tgId] = 0L
      } get StudentTable.id
    }.value.toStudentId()

  override fun resolveStudent(studentId: StudentId): Result<Student, ResolveError<StudentId>> =
    transaction(database) {
      val row = StudentTable
        .selectAll()
        .where(StudentTable.id eq studentId.id)
        .singleOrNull() ?: return@transaction Err(ResolveError(studentId))
      Ok(
        Student(
          studentId,
          row[StudentTable.name],
          row[StudentTable.name],
        ),
      )
    }
}

package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.ParentStudents
import com.github.heheteam.commonlib.database.tables.StudentTable
import com.github.michaelbull.result.*
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
              it[StudentTable.tgId],
            )
          }.single()
      }
    }
  }

  override fun createStudent(
    name: String,
    surname: String,
    tgId: Long,
  ): StudentId =
    transaction(database) {
      StudentTable.insert {
        it[StudentTable.name] = name
        it[StudentTable.surname] = surname
        it[StudentTable.tgId] = tgId
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
          row[StudentTable.surname],
          row[StudentTable.tgId],
        ),
      )
    }

  override fun resolveByTgId(tgId: UserId): Result<Student, ResolveError<UserId>> {
    return transaction(database) {
      val maybeRow = StudentTable
        .selectAll()
        .where { StudentTable.tgId eq (tgId.chatId.long) }
        .limit(1)
        .firstOrNull().toResultOr {
          ResolveError(tgId, Student::class.simpleName)
        }
      maybeRow.map { row ->
        Student(
          row[StudentTable.id].value.toStudentId(),
          row[StudentTable.name],
          row[StudentTable.surname],
          row[StudentTable.tgId],
        )
      }
    }
  }

  override fun updateTgId(
    studentId: StudentId,
    tgId: UserId,
  ): Result<Unit, ResolveError<StudentId>> {
    val updated = transaction {
      StudentTable.update({ StudentTable.id eq studentId.id }) {
        it[StudentTable.tgId] = tgId.chatId.long
      }
    }
    return if (updated == 1) Ok(Unit) else Err(ResolveError(studentId))
  }
}

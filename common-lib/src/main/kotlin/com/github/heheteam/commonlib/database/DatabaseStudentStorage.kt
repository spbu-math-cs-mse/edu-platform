package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.BindError
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.asEduPlatformError
import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.util.toRawChatId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseStudentStorage(val database: Database) : StudentStorage {
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
          it[ParentStudents.studentId] = studentId.long
          it[ParentStudents.parentId] = parentId.long
        }
      }
      Ok(Unit)
    } catch (e: Throwable) {
      Err(BindError(studentId, parentId, causedBy = e.asEduPlatformError()))
    }

  override fun getChildren(parentId: ParentId): List<Student> {
    val ids =
      transaction(database) {
          ParentStudents.selectAll().where(ParentStudents.parentId eq parentId.long)
        }
        .map { it[ParentStudents.studentId].value }
    return transaction(database) {
      return@transaction ids.map { studentId ->
        StudentTable.selectAll()
          .where(StudentTable.id eq studentId)
          .map {
            Student(
              studentId.toStudentId(),
              it[StudentTable.name],
              it[StudentTable.name],
              it[StudentTable.tgId].toRawChatId(),
            )
          }
          .single()
      }
    }
  }

  override fun createStudent(name: String, surname: String, tgId: Long): StudentId =
    transaction(database) {
        StudentTable.insert {
          it[StudentTable.name] = name
          it[StudentTable.surname] = surname
          it[StudentTable.tgId] = tgId
        } get StudentTable.id
      }
      .value
      .toStudentId()

  override fun resolveStudent(studentId: StudentId): Result<Student, ResolveError<StudentId>> =
    transaction(database) {
      val row =
        StudentTable.selectAll().where(StudentTable.id eq studentId.long).singleOrNull()
          ?: return@transaction Err(ResolveError(studentId))
      Ok(
        Student(
          studentId,
          row[StudentTable.name],
          row[StudentTable.surname],
          row[StudentTable.tgId].toRawChatId(),
        )
      )
    }

  override fun resolveByTgId(tgId: UserId): Result<Student, ResolveError<UserId>> {
    return transaction(database) {
      val maybeRow =
        StudentTable.selectAll()
          .where { StudentTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          .toResultOr { ResolveError(tgId, Student::class.simpleName) }
      maybeRow.map { row ->
        Student(
          row[StudentTable.id].value.toStudentId(),
          row[StudentTable.name],
          row[StudentTable.surname],
          row[StudentTable.tgId].toRawChatId(),
        )
      }
    }
  }

  override fun updateTgId(
    studentId: StudentId,
    newTgId: UserId,
  ): Result<Unit, ResolveError<StudentId>> {
    val rows =
      transaction(database) {
        StudentTable.update({ StudentTable.id eq studentId.long }) {
          it[StudentTable.tgId] = newTgId.chatId.long
        }
      }
    return if (rows == 1) {
      Ok(Unit)
    } else {
      Err(ResolveError(studentId))
    }
  }
}

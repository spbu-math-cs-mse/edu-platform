package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.errors.BindError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.errors.asEduPlatformError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.heheteam.commonlib.util.toRawChatId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
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
      Err(
        BindError(
          studentId,
          parentId,
          causedBy = e.asEduPlatformError(DatabaseStudentStorage::class),
        )
      )
    }

  override fun getChildren(parentId: ParentId): Result<List<Student>, EduPlatformError> = binding {
    val ids =
      catchingTransaction(database) {
          ParentStudents.selectAll().where(ParentStudents.parentId eq parentId.long).map {
            it[ParentStudents.studentId].value
          }
        }
        .bind()

    catchingTransaction(database) {
        ids.map { studentId ->
          StudentTable.selectAll()
            .where(StudentTable.id eq studentId)
            .map {
              Student(
                studentId.toStudentId(),
                it[StudentTable.name],
                it[StudentTable.name],
                it[StudentTable.tgId].toRawChatId(),
                it[StudentTable.lastQuestState],
              )
            }
            .single()
        }
      }
      .bind()
  }

  override fun createStudent(
    name: String,
    surname: String,
    tgId: Long,
    grade: Int?,
    from: String?,
  ): Result<StudentId, EduPlatformError> =
    catchingTransaction(database) {
        StudentTable.insert {
          it[StudentTable.name] = name
          it[StudentTable.surname] = surname
          it[StudentTable.tgId] = tgId
          it[StudentTable.grade] = grade
          it[StudentTable.discoverySource] = from
        } get StudentTable.id
      }
      .map { it.value.toStudentId() }

  override fun resolveStudent(studentId: StudentId): Result<Student?, EduPlatformError> =
    catchingTransaction(database) {
      val row =
        StudentTable.selectAll().where(StudentTable.id eq studentId.long).singleOrNull()
          ?: return@catchingTransaction null
      Student(
        studentId,
        row[StudentTable.name],
        row[StudentTable.surname],
        row[StudentTable.tgId].toRawChatId(),
        row[StudentTable.lastQuestState],
      )
    }

  override fun resolveByTgId(tgId: UserId): Result<Student?, EduPlatformError> {
    return catchingTransaction(database) {
      val row =
        StudentTable.selectAll().where { StudentTable.tgId eq (tgId.chatId.long) }.firstOrNull()
          ?: return@catchingTransaction null
      Student(
        row[StudentTable.id].value.toStudentId(),
        row[StudentTable.name],
        row[StudentTable.surname],
        row[StudentTable.tgId].toRawChatId(),
        row[StudentTable.lastQuestState],
      )
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

  override fun updateLastQuestState(
    studentId: StudentId,
    lastQuestState: String,
  ): Result<Unit, EduPlatformError> {
    val rows =
      transaction(database) {
        StudentTable.update({ StudentTable.id eq studentId.long }) {
          it[StudentTable.lastQuestState] = lastQuestState
        }
      }
    return if (rows == 1) {
      Ok(Unit)
    } else {
      Err(ResolveError(studentId))
    }
  }
}

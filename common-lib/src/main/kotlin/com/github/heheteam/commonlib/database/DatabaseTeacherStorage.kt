package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseTeacherStorage(val database: Database) : TeacherStorage {
  init {
    transaction(database) {
      SchemaUtils.create(TeacherTable)
      SchemaUtils.create(StudentTable)
      SchemaUtils.create(ParentStudents)
    }
  }

  override fun createTeacher(name: String, surname: String, tgId: Long): TeacherId =
    transaction(database) {
        TeacherTable.insert {
          it[TeacherTable.name] = name
          it[TeacherTable.surname] = surname
          it[TeacherTable.tgId] = tgId
        } get TeacherTable.id
      }
      .value
      .toTeacherId()

  override fun resolveTeacher(teacherId: TeacherId): Result<Teacher, ResolveError<TeacherId>> =
    transaction(database) {
      val row =
        TeacherTable.selectAll().where(TeacherTable.id eq teacherId.long).singleOrNull()
          ?: return@transaction Err(ResolveError(teacherId))
      Ok(
        Teacher(
          teacherId,
          row[TeacherTable.name],
          row[TeacherTable.surname],
          RawChatId(row[TeacherTable.tgId]),
        )
      )
    }

  override fun getTeachers(): Result<List<Teacher>, EduPlatformError> =
    catchingTransaction(database) {
      TeacherTable.selectAll().map {
        Teacher(
          TeacherId(it[TeacherTable.id].value),
          it[TeacherTable.name],
          it[TeacherTable.surname],
          RawChatId(it[TeacherTable.tgId]),
        )
      }
    }

  override fun resolveByTgId(tgId: UserId): Result<Teacher?, EduPlatformError> =
    catchingTransaction(database) {
      val row =
        TeacherTable.selectAll().where(TeacherTable.tgId eq tgId.chatId.long).firstOrNull()
          ?: return@catchingTransaction null
      Teacher(
        row[TeacherTable.id].value.toTeacherId(),
        row[TeacherTable.name],
        row[TeacherTable.surname],
        RawChatId(row[TeacherTable.tgId]),
      )
    }

  override fun updateTgId(
    teacherId: TeacherId,
    newTgId: UserId,
  ): Result<Unit, ResolveError<TeacherId>> {
    val rows =
      transaction(database) {
        TeacherTable.update({ TeacherTable.id eq teacherId.long }) {
          it[TeacherTable.tgId] = newTgId.chatId.long
        }
      }
    return if (rows == 1) {
      Ok(Unit)
    } else {
      Err(ResolveError(teacherId))
    }
  }
}

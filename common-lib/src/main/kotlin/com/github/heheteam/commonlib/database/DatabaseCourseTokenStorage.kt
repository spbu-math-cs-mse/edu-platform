package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.table.CourseTokenTable
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseTokenStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TokenError
import com.github.heheteam.commonlib.interfaces.toCourseId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.util.UUID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseCourseTokenStorage(val database: Database) : CourseTokenStorage {
  init {
    transaction(database) { SchemaUtils.create(CourseTokenTable) }
  }

  override fun createToken(courseId: CourseId): String {
    val token = UUID.randomUUID().toString()
    transaction(database) {
      CourseTokenTable.insert {
        it[CourseTokenTable.token] = token
        it[CourseTokenTable.courseId] = courseId.long
        it[CourseTokenTable.usedByStudentId] = null
      }
    }
    return token
  }

  override fun getCourseIdByToken(token: String): Result<CourseId, TokenError> =
    transaction(database) {
      val row =
        CourseTokenTable.selectAll().where(CourseTokenTable.token eq token).singleOrNull()
          ?: return@transaction Err(TokenError.TokenNotFound)

      val usedByStudentId = row[CourseTokenTable.usedByStudentId]
      if (usedByStudentId != null) {
        return@transaction Err(TokenError.TokenAlreadyUsed)
      }

      Ok(row[CourseTokenTable.courseId].value.toCourseId())
    }

  override fun useToken(token: String, studentId: StudentId): Result<Unit, TokenError> =
    transaction(database) {
      val row =
        CourseTokenTable.selectAll().where(CourseTokenTable.token eq token).singleOrNull()
          ?: return@transaction Err(TokenError.TokenNotFound)

      val usedByStudentId = row[CourseTokenTable.usedByStudentId]
      if (usedByStudentId != null) {
        if (usedByStudentId == studentId.long) {
          return@transaction Ok(Unit)
        } else {
          return@transaction Err(TokenError.TokenAlreadyUsed)
        }
      }

      val updated =
        CourseTokenTable.update({ CourseTokenTable.token eq token }) {
          it[CourseTokenTable.usedByStudentId] = studentId.long
        }

      if (updated == 1) {
        Ok(Unit)
      } else {
        Err(TokenError.TokenNotFound)
      }
    }

  override fun deleteToken(token: String): Result<Unit, TokenError> =
    transaction(database) {
      val deleted = CourseTokenTable.deleteWhere { CourseTokenTable.token eq token }
      if (deleted == 1) {
        Ok(Unit)
      } else {
        Err(TokenError.TokenNotFound)
      }
    }
}

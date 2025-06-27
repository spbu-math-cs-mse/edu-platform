package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.table.CourseTokenTable
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.TokenError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.toCourseId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseCourseTokenStorage(val database: Database) {
  init {
    transaction(database) { SchemaUtils.create(CourseTokenTable) }
  }

  fun storeToken(courseId: CourseId, token: String): String {
    transaction(database) {
      CourseTokenTable.insert {
        it[CourseTokenTable.token] = token
        it[CourseTokenTable.courseId] = courseId.long
      }
    }
    return token
  }

  fun setNewToken(courseId: CourseId, newToken: String): String {
    transaction(database) {
      CourseTokenTable.deleteWhere { CourseTokenTable.courseId eq courseId.long }
      CourseTokenTable.insert {
        it[CourseTokenTable.token] = newToken
        it[CourseTokenTable.courseId] = courseId.long
      }
    }
    return newToken
  }

  fun getCourseIdByToken(token: String): Result<CourseId, TokenError> =
    transaction(database) {
      val row =
        CourseTokenTable.selectAll().where(CourseTokenTable.token eq token).singleOrNull()
          ?: return@transaction Err(TokenError.TokenNotFound)

      Ok(row[CourseTokenTable.courseId].value.toCourseId())
    }

  fun doesTokenExist(token: String): Result<Boolean, EduPlatformError> =
    catchingTransaction(database) {
      CourseTokenTable.selectAll().where(CourseTokenTable.token eq token).empty().not()
    }

  fun deleteToken(token: String): Result<Unit, EduPlatformError> =
    catchingTransaction(database) {
      val deleted = CourseTokenTable.deleteWhere { CourseTokenTable.token eq token }
      if (deleted == 1) {
        Ok(Unit)
      } else {
        Err(TokenError.TokenNotFound)
      }
    }

  fun getTokenForCourse(courseId: CourseId): String? =
    transaction(database) {
      val row =
        CourseTokenTable.selectAll()
          .where(CourseTokenTable.courseId eq courseId.long)
          .singleOrNull() ?: return@transaction null

      row[CourseTokenTable.token]
    }
}

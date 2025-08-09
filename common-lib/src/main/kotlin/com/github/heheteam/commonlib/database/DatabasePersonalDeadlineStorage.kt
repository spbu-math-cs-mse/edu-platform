package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.table.PersonalDeadlineTable
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.PersonalDeadlineStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabasePersonalDeadlineStorage(val database: Database) : PersonalDeadlineStorage {
  init {
    transaction(database) { SchemaUtils.create(PersonalDeadlineTable) }
  }

  override fun resolveDeadline(studentId: StudentId): LocalDateTime? =
    transaction(database) {
      PersonalDeadlineTable.selectAll()
        .where(PersonalDeadlineTable.studentId eq studentId.long)
        .firstOrNull()
        ?.get(PersonalDeadlineTable.deadline)
    }

  override fun updateDeadlineForStudent(
    studentId: StudentId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError> {
    return catchingTransaction(database) {
      val existingRecord =
        PersonalDeadlineTable.selectAll()
          .where(PersonalDeadlineTable.studentId eq studentId.long)
          .firstOrNull()

      if (existingRecord != null) {
        PersonalDeadlineTable.update({ PersonalDeadlineTable.studentId eq studentId.long }) {
          it[deadline] = newDeadline
        }
      } else {
        PersonalDeadlineTable.insert {
          it[PersonalDeadlineTable.studentId] = studentId.long
          it[deadline] = newDeadline
        }
      }
    }
  }
}

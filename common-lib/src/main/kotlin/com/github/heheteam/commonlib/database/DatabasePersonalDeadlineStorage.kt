package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.table.PersonalDeadlineTable
import com.github.heheteam.commonlib.interfaces.PersonalDeadlineStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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

  override fun updateDeadlineForStudent(studentId: StudentId, newDeadline: LocalDateTime) {
    return transaction(database) {
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

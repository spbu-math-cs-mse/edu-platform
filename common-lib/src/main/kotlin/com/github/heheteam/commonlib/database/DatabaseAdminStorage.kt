package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.database.table.AdminTable
import com.github.heheteam.commonlib.database.table.AdminWhitelistTable
import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.errors.AdminIsNotWhitelistedError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AdminStorage
import com.github.heheteam.commonlib.interfaces.toAdminId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.toRawChatId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAdminStorage(val database: Database) : AdminStorage {
  init {
    transaction(database) {
      SchemaUtils.create(TeacherTable)
      SchemaUtils.create(StudentTable)
      SchemaUtils.create(ParentStudents)
    }
  }

  override fun addTgIdToWhitelist(tgId: Long): Result<Unit, EduPlatformError> =
    transaction(database) {
      AdminWhitelistTable.insert { it[AdminWhitelistTable.tgId] = tgId }
      Ok(Unit)
    }

  override fun tgIdIsInWhitelist(tgId: Long): Result<Boolean, EduPlatformError> =
    catchingTransaction(database) {
      AdminWhitelistTable.selectAll().where { AdminWhitelistTable.tgId eq tgId }.count() > 0L
    }

  override fun createAdmin(
    name: String,
    surname: String,
    tgId: Long,
  ): Result<AdminId, EduPlatformError> =
    transaction(database) {
      if (
        AdminWhitelistTable.selectAll().where { AdminWhitelistTable.tgId eq tgId }.count() == 0L
      ) {
        return@transaction Err(AdminIsNotWhitelistedError(tgId))
      }

      (AdminTable.insert {
          it[AdminTable.name] = name
          it[AdminTable.surname] = surname
          it[AdminTable.tgId] = tgId
        } get AdminTable.id)
        .value
        .toAdminId()
        .ok()
    }

  override fun resolveByTgId(tgId: UserId): Result<Admin?, EduPlatformError> =
    catchingTransaction(database) {
      val row =
        AdminTable.selectAll().where { AdminTable.tgId eq (tgId.chatId.long) }.firstOrNull()
          ?: return@catchingTransaction null
      Admin(
        row[AdminTable.id].value.toAdminId(),
        row[AdminTable.name],
        row[AdminTable.surname],
        row[AdminTable.tgId].toRawChatId(),
      )
    }

  override fun getAdmins(): Result<List<Admin>, EduPlatformError> =
    transaction(database) {
        AdminTable.selectAll().map {
          Admin(
            it[AdminTable.id].value.toAdminId(),
            it[AdminTable.name],
            it[AdminTable.surname],
            it[AdminTable.tgId].toRawChatId(),
          )
        }
      }
      .let { Ok(it) }
}

package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.database.table.AdminTable
import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AdminStorage
import com.github.heheteam.commonlib.interfaces.toAdminId
import com.github.heheteam.commonlib.util.toRawChatId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseAdminStorage(val database: Database) : AdminStorage {
  init {
    transaction(database) {
      SchemaUtils.create(TeacherTable)
      SchemaUtils.create(StudentTable)
      SchemaUtils.create(ParentStudents)
    }
  }

  override fun createAdmin(name: String, surname: String, tgId: Long): AdminId =
    transaction(database) {
        AdminTable.insert {
          it[AdminTable.name] = name
          it[AdminTable.surname] = surname
          it[AdminTable.tgId] = tgId
        } get AdminTable.id
      }
      .value
      .toAdminId()

  override fun resolveAdmin(adminId: AdminId): Result<Admin, ResolveError<AdminId>> =
    transaction(database) {
      val row =
        AdminTable.selectAll().where(AdminTable.id eq adminId.long).singleOrNull()
          ?: return@transaction Err(ResolveError(adminId))
      Ok(
        Admin(
          adminId,
          row[AdminTable.name],
          row[AdminTable.surname],
          RawChatId(row[AdminTable.tgId]),
        )
      )
    }

  override fun resolveByTgId(tgId: UserId): Result<Admin, ResolveError<UserId>> {
    return transaction(database) {
      val maybeRow =
        AdminTable.selectAll()
          .where { AdminTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          .toResultOr { ResolveError(tgId, Admin::class.simpleName) }
      maybeRow.map { row ->
        Admin(
          row[AdminTable.id].value.toAdminId(),
          row[AdminTable.name],
          row[AdminTable.surname],
          row[AdminTable.tgId].toRawChatId(),
        )
      }
    }
  }

  override fun updateTgId(adminId: AdminId, newTgId: UserId): Result<Unit, ResolveError<AdminId>> {
    val rows =
      transaction(database) {
        AdminTable.update({ AdminTable.id eq adminId.long }) {
          it[AdminTable.tgId] = newTgId.chatId.long
        }
      }
    return if (rows == 1) {
      Ok(Unit)
    } else {
      Err(ResolveError(adminId))
    }
  }

  override fun getAdmins(): List<Admin> =
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
}

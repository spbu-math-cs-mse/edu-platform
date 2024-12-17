package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AdminTable
import com.github.heheteam.commonlib.database.tables.ParentTable
import com.github.heheteam.commonlib.database.tables.TeacherTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAdminIdRegistry(
  val database: Database,
) : AdminIdRegistry {
  override fun getUserId(tgId: UserId): Result<AdminId, ResolveError<UserId>> {
    val adminId =
      transaction(database) {
        AdminTable
          .select(AdminTable.id)
          .where { AdminTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          ?.get(AdminTable.id)
          ?.value
      } ?: return Err(ResolveError(tgId, AdminId::class.simpleName))
    return Ok(AdminId(adminId))
  }
}

class DatabaseTeacherIdRegistry(
  val database: Database,
) : TeacherIdRegistry {

  override fun getUserId(tgId: UserId): Result<TeacherId, ResolveError<UserId>> {
    val teacherId =
      transaction(database) {
        TeacherTable
          .select(TeacherTable.id)
          .where { TeacherTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          ?.get(TeacherTable.id)
          ?.value
      } ?: return Err(ResolveError(tgId, TeacherId::class.simpleName))
    return Ok(TeacherId(teacherId))
  }
}

class DatabaseParentIdRegistry(
  val database: Database,
) : ParentIdRegistry {
  override fun getUserId(tgId: UserId): Result<ParentId, ResolveError<UserId>> {
    val parentId =
      transaction(database) {
        ParentTable
          .select(ParentTable.id)
          .where { ParentTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          ?.get(ParentTable.id)
          ?.value
      } ?: return Err(ResolveError(tgId, ParentId::class.simpleName))
    return Ok(ParentId(parentId))
  }
}

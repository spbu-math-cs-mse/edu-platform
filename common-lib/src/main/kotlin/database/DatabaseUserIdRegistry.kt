package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AdminTable
import com.github.heheteam.commonlib.database.tables.ParentTable
import com.github.heheteam.commonlib.database.tables.StudentTable
import com.github.heheteam.commonlib.database.tables.TeacherTable
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAdminIdRegistry(
  val database: Database,
) : AdminIdRegistry {
  override fun getUserId(tgId: UserId): AdminId? {
    val adminId =
      transaction(database) {
        AdminTable
          .select(AdminTable.id)
          .where { AdminTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          ?.get(AdminTable.id)
          ?.value
      } ?: return null
    return AdminId(adminId)
  }
}

class DatabaseStudentIdRegistry(
  val database: Database,
) : StudentIdRegistry {
  override fun getUserId(tgId: UserId): StudentId? {
    val studentId =
      transaction(database) {
        StudentTable
          .select(StudentTable.id)
          .where { StudentTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          ?.get(StudentTable.id)
          ?.value
      } ?: return null
    return StudentId(studentId)
  }
}

class DatabaseTeacherIdRegistry(
  val database: Database,
) : TeacherIdRegistry {

  override fun getUserId(tgId: UserId): TeacherId? {
    val teacherId =
      transaction(database) {
        TeacherTable
          .select(TeacherTable.id)
          .where { TeacherTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          ?.get(TeacherTable.id)
          ?.value
      } ?: return null
    return TeacherId(teacherId)
  }
}

class DatabaseParentIdRegistry(
  val database: Database,
) : ParentIdRegistry {
  override fun getUserId(tgId: UserId): ParentId? {
    val parentId =
      transaction(database) {
        ParentTable
          .select(ParentTable.id)
          .where { ParentTable.tgId eq (tgId.chatId.long) }
          .limit(1)
          .firstOrNull()
          ?.get(ParentTable.id)
          ?.value
      } ?: return null
    return ParentId(parentId)
  }
}

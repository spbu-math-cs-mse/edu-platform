package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.api.UserIdRegistry
import com.github.heheteam.commonlib.database.tables.StudentTable
import com.github.heheteam.commonlib.database.tables.TeacherTable
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.union

class DatabaseUserIdRegistry(
  val database: Database,
) : UserIdRegistry {
  override fun getUserId(tgId: UserId): String? =
    transaction(database) {
      val students =
        StudentTable
          .select(StudentTable.id)
          .where { StudentTable.tgId eq (tgId.chatId.long) }
      val teachers =
        TeacherTable
          .select(TeacherTable.id)
          .where { TeacherTable.tgId eq (tgId.chatId.long) }
//      val admins = TODO()
//      val parents = TODO()

      students
        .union(teachers)
        .limit(1)
        .firstOrNull()
        ?.get(StudentTable.id)
        ?.value
        ?.toString()
    }
}

package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object CourseTable : LongIdTable("course") {
  val name = varchar("name", 255)
  val spreadsheetId = varchar("spreadsheetId", 255).nullable()
  val groupRawChatId = long("groupChatId").nullable()
}

package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object TeacherMenuMessageTable : LongIdTable() {
  val chatId = long("chatId")
  val messageId = long("messageId")
}

package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object SolutionPersonalMessagesTable : LongIdTable() {
  val solutionId = reference("solutionId", SolutionTable.id)
  val chatId = long("chatId").nullable()
  val messageId = long("messageId").nullable()
}

object SolutionGroupMessagesTable : LongIdTable() {
  val solutionId = reference("solutionId", SolutionTable.id)
  val chatId = long("chatId")
  val messageId = long("messageId")
}

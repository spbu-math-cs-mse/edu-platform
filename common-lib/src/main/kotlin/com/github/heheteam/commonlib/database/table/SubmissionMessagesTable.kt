package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object SubmissionPersonalMessagesTable : LongIdTable() {
  val submissionId = reference("submissionId", SubmissionTable.id)
  val chatId = long("chatId")
  val messageId = long("messageId")
}

object SubmissionGroupMessagesTable : LongIdTable() {
  val submissionId = reference("submissionId", SubmissionTable.id)
  val chatId = long("chatId")
  val messageId = long("messageId")
}

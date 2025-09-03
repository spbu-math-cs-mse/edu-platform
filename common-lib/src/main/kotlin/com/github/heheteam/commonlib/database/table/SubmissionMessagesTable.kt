package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object SubmissionPersonalMessagesTable : LongIdTable() {
  val submissionId =
    reference("submissionId", SubmissionTable.id, onDelete = ReferenceOption.CASCADE)
  val chatId = long("chatId")
  val messageId = long("messageId")
}

object SubmissionGroupMessagesTable : LongIdTable() {
  val submissionId =
    reference("submissionId", SubmissionTable.id, onDelete = ReferenceOption.CASCADE)
  val chatId = long("chatId")
  val messageId = long("messageId")
}

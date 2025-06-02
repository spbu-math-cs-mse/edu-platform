package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SentMessageLogTable : LongIdTable("sent_message_log") {
  val scheduledMessageId = long("scheduled_message_id").references(ScheduledMessageTable.id)
  val studentId = long("student_id").references(StudentTable.id)
  val sentTimestamp = datetime("sent_timestamp")
  val telegramMessageId = long("telegram_message_id")
  val chatId = long("chat_id")
}

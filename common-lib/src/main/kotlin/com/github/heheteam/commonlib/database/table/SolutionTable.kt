package com.github.heheteam.commonlib.database.table

import com.github.heheteam.commonlib.TelegramAttachment
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SolutionTable : LongIdTable("solution") {
  val studentId = reference("studentId", StudentTable.id)
  val chatId = long("chatId")
  val messageId = long("messageId")
  val problemId = reference("problemId", ProblemTable.id)
  val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
  val attachments = json<TelegramAttachment>("attachments", Json)
}

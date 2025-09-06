package com.github.heheteam.commonlib.database.table

import com.github.heheteam.commonlib.TextWithMediaAttachments
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SubmissionTable : LongIdTable("submission") {
  val studentId = reference("studentId", StudentTable.id)
  val chatId = long("chatId")
  val messageId = long("messageId")
  val problemId = reference("problemId", ProblemTable.id, onDelete = ReferenceOption.CASCADE)
  val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
  val submissionContent = json<TextWithMediaAttachments>("content", Json)
  val responsibleTeacher = reference("responsibleTeacher", TeacherTable.id).nullable()
}

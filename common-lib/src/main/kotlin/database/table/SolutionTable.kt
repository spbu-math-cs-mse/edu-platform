package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SolutionTable : LongIdTable("solution") {
  val studentId = reference("studentId", StudentTable.id)
  val problemId = reference("problemId", ProblemTable.id)
  val chatId = long("chatId")
  val messageId = long("messageId")
  val content = varchar("content", 200).nullable()
  val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
}

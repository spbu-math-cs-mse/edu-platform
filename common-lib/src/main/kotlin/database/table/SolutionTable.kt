package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SolutionTable : IntIdTable("solution") {
  val studentId = reference("studentId", StudentTable.id)
  val chatId = integer("chatId")
  val messageId = integer("messageId")
  val problemId = reference("problemId", ProblemTable.id)
  val content = varchar("content", 200)
  val timestamp = datetime("timestamp")
}
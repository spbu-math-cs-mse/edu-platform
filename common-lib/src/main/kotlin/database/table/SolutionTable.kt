package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SolutionTable : LongIdTable("solution") {
    val studentId = reference("studentId", StudentTable.id)
    val chatId = long("chatId")
    val messageId = long("messageId")
    val problemId = reference("problemId", ProblemTable.id)
    val content = varchar("content", 200)
    val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
}

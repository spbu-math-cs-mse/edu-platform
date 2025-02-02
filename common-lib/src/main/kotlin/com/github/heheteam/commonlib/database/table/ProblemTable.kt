package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ProblemTable : LongIdTable("problem") {
  val number = varchar("number", 64)
  val description = text("description")
  val maxScore = integer("maxScore")
  val assignmentId = reference("assignmentId", AssignmentTable.id)
  val deadline = datetime("deadline").nullable()
}

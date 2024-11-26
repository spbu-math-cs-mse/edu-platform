package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object ProblemTable : IntIdTable("problem") {
  val description = varchar("description", 100)
  val maxScore = integer("maxScore")
  val assignment = reference("assignmentId", AssignmentTable.id)
}
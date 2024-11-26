package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object AssignmentTable : LongIdTable("assignment") {
  val description = varchar("description", 100)
  val course = reference("courseId", CourseTable.id)
}

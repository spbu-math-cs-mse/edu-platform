package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object AssignmentTable : IntIdTable("assignment") {
  val description = varchar("description", 100)
  val course = reference("courseId", CourseTable.id)
}

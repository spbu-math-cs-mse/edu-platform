package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object AssignmentTable : LongIdTable("assignment") {
  val description = varchar("description", 100)
  val courseId = reference("courseId", CourseTable.id)
}

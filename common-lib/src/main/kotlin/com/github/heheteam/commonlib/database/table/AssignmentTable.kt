package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object AssignmentTable : LongIdTable("assignment") {
  val serialNumber = integer("serialNumber")
  val description = varchar("description", 100)
  val courseId = reference("courseId", CourseTable.id)
}

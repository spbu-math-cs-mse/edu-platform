package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

@Suppress("MagicNumber")
object ChallengeTable : LongIdTable("challenge") {
  val description = varchar("description", 100)
  val courseId = reference("courseId", CourseTable.id)
  val assignmentId = reference("assignmentId", AssignmentTable.id)
}

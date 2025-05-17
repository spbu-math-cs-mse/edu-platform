package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object CourseTokenTable : LongIdTable("course_token") {
  val token = varchar("token", 255).uniqueIndex()
  val courseId = reference("course_id", CourseTable)
  val usedByStudentId = long("used_by_student_id").nullable()
}

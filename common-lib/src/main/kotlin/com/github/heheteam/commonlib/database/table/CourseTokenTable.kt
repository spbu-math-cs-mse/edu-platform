package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

private const val TOKEN_LENGTH: Int = 255

object CourseTokenTable : LongIdTable("course_token") {
  val token = varchar("token", TOKEN_LENGTH).uniqueIndex()
  val courseId = reference("course_id", CourseTable)
}

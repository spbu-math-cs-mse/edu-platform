package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object CourseStudents : LongIdTable("courseStudents") {
  val studentId = reference("studentId", StudentTable.id)
  val courseId = reference("courseId", CourseTable.id)
}

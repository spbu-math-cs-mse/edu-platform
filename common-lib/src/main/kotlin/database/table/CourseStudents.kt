package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object CourseStudents : IntIdTable("courseStudents") {
  val studentId = reference("studentId", StudentTable.id)
  val courseId = reference("courseId", CourseTable.id)
}
